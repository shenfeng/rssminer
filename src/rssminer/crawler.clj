(ns rssminer.crawler
  (:use [rssminer.util :only [md5-sum assoc-if]]
        [rssminer.db.util :only [parse-timestamp]]
        [rssminer.time :only [now-seconds]]
        [clojure.tools.logging :only [info]])
  (:require [rssminer.db.crawler :as db]
            [rssminer.http :as http]
            [rssminer.config :as conf])
  (:import crawler.CrawlerThreadFactory
           [java.util Queue LinkedList Date]
           [java.util.concurrent Executors ExecutorService
            TimeUnit ThreadFactory]))

(def running? (atom false))
(def fetch-size 100)
(def ^Queue queue (LinkedList.))

(defn- extract-and-save-links [referer html]
  (let [{:keys [rss links]} (http/extract-links (:url referer) html)]
    (doseq [{:keys [url title]} rss]
      (db/insert-rss-link {:url url
                           :title title
                           :crawler_link_id (:id referer)}))
    (db/insert-crawler-links
     referer (filter #(.startsWith ^String (:url %) "http://") links))))

(defn crawl-link
  [{:keys [id url last_md5 check_interval] :as referer}]
  (let [{:keys [status headers body] :as resp} (http/get url)
        html (when body (slurp body))
        md5 (when html (md5-sum html))]
    (db/update-crawler-link
     id (assoc-if {}
                  :next_check_ts
                  (+ (now-seconds) check_interval)
                  :last_md5 md5
                  :last_modified (:last_modified headers)
                  :server (:server headers)))
    (when (and html (not= md5 last_md5))
      (extract-and-save-links referer html))))

(defn get-next-link []
  (let [newly (when-not (.peek queue)
                (db/fetch-crawler-links fetch-size))]
    (locking queue
      (if (.peek queue) ;; has element?
        (.poll queue)   ;; retrieves and removes
        (let [links (or newly (db/fetch-crawler-links fetch-size))]
          (when links
            (doseq [link links]
              (.offer queue link))
            (get-next-link)))))))

(defn start-crawler [& {:keys [threads]}]
  (let [threads (or threads conf/crawler-threads-count)
        ^ExecutorService exec (Executors/newFixedThreadPool
                               threads (CrawlerThreadFactory.))
        ^Runnable task (fn [] (loop [link (get-next-link)]
                               (when (and link @running?)
                                 (crawl-link link)
                                 (recur (get-next-link)))))
        shutdown #(do (info "shutdown crawler....")
                      (reset! running? true))
        wait #(do (.awaitTermination exec Integer/MAX_VALUE TimeUnit/MINUTES)
                  (info "crawler shutdowned."))]
    (reset! running? true)
    (info "starting crawler")
    (dotimes [_ threads]
      (.submit exec task))
    (.shutdownNow exec)
    (fn [op]
      (case op
        :wait (wait)
        :shutdown (shutdown)
        :shutdown-wait (do (shutdown) (wait))))))
