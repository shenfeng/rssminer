(ns rssminer.crawler
(:use (rssminer [time :only [now-seconds]]
                [util :only [assoc-if]])
      [clojure.tools.logging :only [info error trace]])
  (:require [rssminer.db.crawler :as db]
            [rssminer.http :as http]
            [rssminer.config :as conf])
(:import [java.util Queue LinkedList]
         [rssminer.task HttpTaskRunner IHttpTask IHttpTaskProvder]
         org.jboss.netty.handler.codec.http.HttpResponse))

(defonce crawler (atom nil))

(defn stop-crawler []
  (when-not (nil? @crawler)
    (.stop ^HttpTaskRunner @crawler)
    (reset! crawler nil)))

;; currently implementation is single thread, so locking is not
;; required.
(defn get-next-link [^Queue queue]
  (if (.peek queue) ;; has element?
    (.poll queue)   ;; retrieves and removes
    (let [links (db/fetch-crawler-links conf/fetch-size)]
      (trace "fetch" (count links) "crawler links from h2")
      (when (seq links)
        (doseq [link links]
          (.offer queue link))
        (get-next-link queue)))))

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
      (let [{:keys [status headers body]} (http/parse-response resp)
            updated (assoc-if (conf/next-check check_interval body)
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

(defn start-crawler [& {:keys [concurrency]}]
  (stop-crawler)
  (let [c (or concurrency conf/crawler-threads-count)]
    (reset! crawler (doto (HttpTaskRunner. (mk-provider) http/client
                                           c "crawler")
                      (.start)))))
