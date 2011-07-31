(ns freader.crawler
  (:use [freader.util :only [extract-links md5-sum]]
        [freader.db.util :only [parse-timestamp]]
        [freader.test-common :only [tracep]]
        clojure.pprint)
  (:require [freader.db.crawler :as db]
            [freader.http :as http]
            [freader.config :as conf])
  (:import [java.util Queue LinkedList Date]
           [java.util.concurrent Executors ExecutorService TimeUnit]))

(defn- extract-and-save-links [referer html]
  (let [{:keys [rss links]} (extract-links (:url referer) html)]
    (doseq [{:keys [href title]} rss]
      (db/insert-rss-link {:url href
                           :title title
                           :crawler_link_id (:id referer)}))
    (db/insert-crawler-links
     referer (filter #(.startsWith (:href %) "http://") links))))

(defn crawl-link
  [{:keys [id url last-md5 last_http_status] :as referer}]
  (let [{:keys [status headers body] :as resp} (http/get url)
        html (when body (slurp body))
        md5 (when html (md5-sum html))]
    (db/update-crawler-link {:id id
                             :last_md5 md5
                             :last_http_status status
                             :last_modified (parse-timestamp
                                             (:last_modified headers))
                             :server (:server headers)})
    (when (and html (not= md5 last-md5))
      (extract-and-save-links referer html))))

(let [^Queue queue (LinkedList.)]
  (defn get-next-link []
    ;; (println "finished " (count (db/get-all)))
    (let [newly (when-not (.peek queue) (db/fetch-crawler-links 50))]
      (locking queue
        (if (.peek queue) ;; has element
          (.poll queue)   ;; retrieves and removes
          (let [links (or newly (db/fetch-crawler-links 30))]
            (if (nil? links)
              nil ;; no more
              (do (doseq [link links]
                    (.offer queue link))
                  (get-next-link)))))))))

(defn start-crawler [& {:keys [threads]}]
  (let [threads (or threads conf/crawler-threads-count)
        ^ExecutorService exec (Executors/newFixedThreadPool threads)
        ^Runnable task (fn [] (loop [link (get-next-link)]
                               (when link
                                 (crawl-link link)
                                 (recur (get-next-link)))))]
    (dotimes [_ threads]
      (.submit exec task))
    (.shutdown exec)
    (.awaitTermination exec Integer/MAX_VALUE TimeUnit/MINUTES)))
