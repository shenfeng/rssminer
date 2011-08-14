(ns rssminer.crawler
  (:use [rssminer.util :only [md5-sum assoc-if tracep]]
        [rssminer.db.util :only [parse-timestamp]]
        [rssminer.time :only [now-seconds]]
        [clojure.tools.logging :only [info error trace]])
  (:require [rssminer.db.crawler :as db]
            [rssminer.http :as http]
            [rssminer.config :as conf])
  (:import crawler.CrawlerThreadFactory
           [java.util Queue LinkedList Date]
           [java.util.concurrent Executors ExecutorService TimeUnit ]))

(def running? (atom false))
(def fetch-size 100)
(def ^Queue queue (LinkedList.))
(defn- rand-ts [] (rand-int 1000000))

(defn- extract-and-save-links [referer html]
  (let [{:keys [rss links]} (http/extract-links (:url referer) html)]
    (doseq [{:keys [url title]} rss]
      (db/insert-rss-link {:url url
                           :title title
                           :next_check_ts (rand-ts)
                           :crawler_link_id (:id referer)}))
    (db/insert-crawler-links (map #(assoc %
                                     :next_check_ts (rand-ts)
                                     :referer_id (:id referer)) links))))

(defn crawl-link
  [{:keys [id url last_md5 check_interval] :as referer}]
  (let [{:keys [status headers body] :as resp} (http/get url)
        html (when body (try (slurp body)
                             (catch Exception e
                               (error e url))))
        md5 (when html (md5-sum html))]
    (db/update-crawler-link
     id (assoc-if {}
                  :next_check_ts (+ (now-seconds) check_interval)
                  :last_md5 md5
                  :last_status status
                  :last_modified (:last_modified headers)
                  :server (:server headers)))
    (when (and html (not= md5 last_md5))
      (extract-and-save-links referer html))))

(defn get-next-link []
  (locking queue
    (if (.peek queue) ;; has element?
      (.poll queue)   ;; retrieves and removes
      (let [links (db/fetch-crawler-links fetch-size)]
        (trace "fetch" (count links) "crawler links from h2")
        (when (seq links)
          (doseq [link links]
            (.offer queue link))
          (get-next-link))))))

(defn start-crawler [& {:keys [threads]}]
  (let [threads (or threads conf/crawler-threads-count)
        ^ExecutorService exec (Executors/newFixedThreadPool
                               threads (CrawlerThreadFactory.))
        ^Runnable task #(loop [link (get-next-link)]
                          (trace "link:" (:url link))
                          (when (and link @running?)
                            (try (crawl-link link)
                                 (catch Exception e
                                   (error e "start-crawler" (:url link))))
                            (recur (get-next-link))))
        shutdown #(do (.shutdownNow exec) (reset! running? false))
        wait #(.awaitTermination exec Integer/MAX_VALUE TimeUnit/MINUTES)]
    (reset! running? true)
    (dotimes [_ threads]
      (.submit exec task))
    (.shutdown exec)
    (fn [op]
      (case op
        :wait (wait)
        :shutdown (shutdown)
        :shutdown-wait (do (shutdown) (wait))))))
