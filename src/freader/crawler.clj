(ns freader.crawler
  (:use [freader.util :only [extract-links md5-sum]]
        [freader.db.util :only [parse-timestamp]]
        [clojure.tools.logging :only [info error]])
  (:require [freader.db.crawler :as db]
            [freader.http :as http]
            [freader.config :as conf])
  (:import [java.util Queue LinkedList Date]
           [java.util.concurrent Executors ExecutorService TimeUnit]))

(def running? (atom false))
(def fetch-size 100)
(def ^Queue queue (LinkedList.))

(defn- extract-and-save-links [referer html]
  (let [{:keys [rss links]} (extract-links (:url referer) html)]
    (doseq [{:keys [href title]} rss]
      (db/insert-rss-link {:url href
                           :title title
                           :crawler_link_id (:id referer)}))
    (db/insert-crawler-links
     referer (filter #(.startsWith ^String (:href %) "http://") links))))

(defn crawl-link
  [{:keys [id url last-md5 last_http_status] :as referer}]
  (let [{:keys [status headers body] :as resp} (http/get url)
        html (when body (slurp body))
        md5 (when html (md5-sum html))]
    (db/update-crawler-link {:id id
                             :last_md5 md5
                             :last_status status
                             :last_modified
                             (when-let [lm (:last_modified headers)]
                               (parse-timestamp lm))
                             :server (:server headers)})
    (when (and html (not= md5 last-md5))
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
  (info "starting crawler")
  (let [threads (or threads conf/crawler-threads-count)
        ^ExecutorService exec (Executors/newFixedThreadPool threads)
        ^Runnable task (fn [] (loop [link (get-next-link)]
                               (when (and link @running?)
                                 (crawl-link link)
                                 (recur (get-next-link)))))]
    (reset! running? true)
    (dotimes [_ threads]
      (.submit exec task))
    (fn []
      (.shutdown exec)
      (reset! running? false)
      (info "shutdown crawler....")
      (.awaitTermination exec Integer/MAX_VALUE TimeUnit/MINUTES)
      (info "crawler shutdowned."))))
