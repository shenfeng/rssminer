(ns rssminer.crawler
  (:use (rssminer [time :only [now-seconds]]
                  [util :only [assoc-if next-check]])
        [clojure.tools.logging :only [info error trace]])
  (:require [rssminer.db.crawler :as db]
            [rssminer.http :as http]
            [rssminer.config :as conf])
  (:import [java.util Queue LinkedList]
           [rssminer.task HttpTaskRunner IHttpTask IHttpTaskProvder]
           org.jboss.netty.handler.codec.http.HttpResponse))

(defonce crawler (atom nil))

(defn running? []
  (if-not (nil? @crawler)
    (.isRunning ^HttpTaskRunner @crawler)
    false))

(defn stop-crawler []
  (when (running?)
    (.stop ^HttpTaskRunner @crawler)))

(defn crawler-stat []
  (when-not (nil? @crawler)
    (.getStat ^HttpTaskRunner @crawler)))

(defn get-next-link [^Queue queue]
  (locking queue
    (if (.peek queue) (.poll queue) ;; retrieves and removes
        (let [links (db/fetch-crawler-links conf/fetch-size)]
          (trace "fetch" (count links) "crawler links from h2")
          (when (seq links)
            (doseq [link links]
              (.offer queue link))
            (get-next-link queue))))))

(defn extract-and-save-links [referer html]
  (let [{:keys [rss links]} (http/extract-links (:url referer) html)]
    (doseq [{:keys [url title]} rss]
      (when-not (and url (re-find #"(?i)\bcomments" (or title "")))
        (db/insert-rss-link {:url url
                             :title title
                             :next_check_ts (conf/rand-ts)
                             :crawler_link_id (:id referer)})))
    (db/insert-crawler-links referer
                             (map #(assoc %
                                     :next_check_ts (conf/rand-ts)
                                     :referer_id (:id referer)) links))))

(defn extract-title [html]
  (when html (-> (re-seq #"(?im)<title>(.+)</title>" html)
                 first
                 second)))

(defn- mk-task [{:keys [id url check_interval last_modified] :as link}]
  (reify IHttpTask
    (getUri [this]
      (java.net.URI. url))
    (getHeaders [this]
      (if last_modified
        {"If-Modified-Since" last_modified} {}))
    (doTask [this ^HttpResponse resp]
      (let [{:keys [status headers body] :as resp} (http/parse-response resp)
            updated (assoc-if (next-check check_interval resp)
                              :last_modified (:last-modified headers)
                              :title (extract-title body))]
        (db/update-crawler-link id updated)
        (when body
          (extract-and-save-links link body))))))

(defn mk-provider []
  (let [queue (LinkedList.)]
    (reify IHttpTaskProvder
      (nextTask [this]
        (mk-task (get-next-link queue))))))

(defn start-crawler [& {:keys [queue worker]}]
  (stop-crawler)
  (let [queue (or queue conf/crawler-queue)]
    (reset! crawler (doto (HttpTaskRunner. (mk-provider) http/client
                                           queue "Crawler")
                      (.start)))))
