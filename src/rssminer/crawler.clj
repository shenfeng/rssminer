(ns rssminer.crawler
  (:use (rssminer [time :only [now-seconds]]
                  [util :only [assoc-if next-check]])
        [clojure.tools.logging :only [info error trace]])
  (:require [rssminer.db.crawler :as db]
            [rssminer.http :as http]
            [rssminer.config :as conf])
  (:import java.util.LinkedList
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

(defn get-next-link [^LinkedList queue]
  (locking queue
    (if (.peek queue) (.poll queue) ;; retrieves and removes
        (let [links (db/fetch-crawler-links conf/fetch-size)]
          (trace "fetch" (count links) "crawler links from h2")
          (if (.addAll queue links)
            (get-next-link queue))))))

(defn extract-and-save-links [referer links rss]
  (doseq [{:keys [url title]} rss]
    (when-not (and url (re-find #"(?i)\bcomments" (or title "")))
      (db/insert-rss-link {:url url
                           :title title
                           :next_check_ts (conf/rand-ts)
                           :crawler_link_id (:id referer)})))
  (db/insert-crawler-links referer
                           (map #(assoc %
                                   :next_check_ts (conf/rand-ts)
                                   :referer_id (:id referer)) links)))

(defn- mk-task [{:keys [id url check_interval last_modified] :as link}]
  (reify IHttpTask
    (getUri [this]
      (java.net.URI. url))
    (getHeaders [this]
      (if last_modified
        {"If-Modified-Since" last_modified} {}))
    (doTask [this resp]
      (let [{:keys [status headers body] :as resp} (http/parse-response resp)
            {:keys [title links rss]} (when body (http/extract-links body))
            updated (assoc-if (next-check check_interval headers)
                              :last_modified (:last-modified headers)
                              :title title)]
        (db/update-crawler-link id updated)
        (when body
          (extract-and-save-links link links rss))))))

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
