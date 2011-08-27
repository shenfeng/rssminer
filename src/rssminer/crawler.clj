(ns rssminer.crawler
  (:use [rssminer.util :only [md5-sum assoc-if start-tasks]]
        [rssminer.db.util :only [parse-timestamp]]
        [rssminer.time :only [now-seconds]]
        [clojure.tools.logging :only [info error trace]])
  (:require [rssminer.db.crawler :as db]
            [rssminer.http :as http]
            [rssminer.config :as conf])
  (:import [java.util Queue LinkedList]))

(def ^Queue queue (LinkedList.))

(defonce crawler (atom nil))

(defn stop-crawler []
  (when-not (nil? @crawler)
    (info "shutdown link crawler....")
    (@crawler :shutdown)
    (reset! crawler nil)))

(defn- extract-and-save-links [referer html]
  (let [{:keys [rss links]} (http/extract-links (:url referer) html)]
    (doseq [{:keys [url title]} rss]
      (db/insert-rss-link {:url url
                           :title title
                           :next_check_ts (conf/rand-ts)
                           :crawler_link_id (:id referer)}))
    (db/insert-crawler-links (map #(assoc %
                                     :next_check_ts (conf/rand-ts)
                                     :referer_id (:id referer)) links))))

(defn crawl-link
  [{:keys [id url last_md5 check_interval] :as referer}]
  (let [{:keys [status headers body] :as resp} (http/get url)
        html (when body (try (slurp body)
                             (catch Exception e
                               (error e url))))
        md5 (when html (md5-sum html))
        next-check (conf/next-check check_interval md5)]
    (trace status url)
    (db/update-crawler-link id (assoc-if next-check
                                :last_md5 md5
                                :last_modified (:last_modified headers)))
    (when (and html (not= md5 last_md5))
      (extract-and-save-links referer html))))

(defn get-next-link []
  (locking queue
    (if (.peek queue) ;; has element?
      (.poll queue)   ;; retrieves and removes
      (let [links (db/fetch-crawler-links conf/fetch-size)]
        (trace "fetch" (count links) "crawler links from h2")
        (when (seq links)
          (doseq [link links]
            (.offer queue link))
          (get-next-link))))))

(defn start-crawler [& {:keys [threads]}]
  (stop-crawler)
  (reset! crawler (start-tasks get-next-link crawl-link "crawler"
                               (or threads conf/crawler-threads-count)))
  (info "link crawler started"))
