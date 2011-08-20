(ns rssminer.fetcher
  (:use [rssminer.util :only [md5-sum assoc-if threadfactory]]
        [clojure.tools.logging :only [info error trace]]
        [rssminer.parser :only [parse-feed]])
  (:require [rssminer.db.feed :as db]
            [rssminer.db.crawler :as cdb]
            [rssminer.http :as http]
            [rssminer.config :as conf])
  (:import [java.util Queue LinkedList Date]
           [java.util.concurrent Executors ExecutorService TimeUnit ]))

(def running? (atom false))

(def ^Queue queue (LinkedList.))

(defn extract-and-save-feeds [html rss-id]
  (if-let [feeds (parse-feed html)]
    (db/save-feeds rss-id feeds nil)))

(defn fetch-rss
  [{:keys [id url check_interval last_modified last_md5] :as link}]
  (let [{:keys [status headers body]} (http/get url
                                                :last_modified last_modified)
        html (when body (try (slurp body)
                             (catch Exception e
                               (error e url))))
        md5 (when html (md5-sum html))
        next-check (conf/next-check check_interval
                                    (and (= 200 status) (not= md5 last_md5)))]
    (trace status url)
    (db/update-rss-link id (assoc-if next-check
                                     :last_md5 md5
                                     :last_modified (:last_modified headers)))
    (when html (extract-and-save-feeds html id))))

(defn get-next-link []
  (locking queue
    (if (.peek queue) ;; has element?
      (.poll queue)   ;; retrieves and removes
      (let [links (cdb/fetch-rss-links conf/fetch-size)]
        (trace "fetch" (count links) "rss links from h2")
        (when (seq links)
          (doseq [link links]
            (.offer queue link))
          (get-next-link))))))

(defn fetcher-crawler [& {:keys [threads]}]
  (let [threads (or threads conf/crawler-threads-count)
        ^ExecutorService exec (Executors/newFixedThreadPool
                               threads (threadfactory "fetcher"))
        ^Runnable task #(loop [link (get-next-link)]
                          (when (and link @running?)
                            (try (fetch-rss link)
                                 (catch Exception e
                                   (error e "fetcher" (:url link))))
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
