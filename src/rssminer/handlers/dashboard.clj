(ns rssminer.handlers.dashboard
  (:use [rssminer.fetcher :only [start-fetcher stop-fetcher fetcher]]
        [rssminer.crawler :only [start-crawler stop-crawler crawler]])
  (:require [rssminer.db.dashboard :as db]
            [rssminer.config :as conf]))

(defn get-data [req]
  (case (-> req :params :q)
    "rsslinks"
    {:caption "Newly added Rss Links"
     :data (or (db/get-rss-links) [])}
    "pending"
    {:caption "Pending Links"
     :data (or (db/get-pending-links) [])}
    "crawled"
    {:caption "Cawled Links"
     :data (or (db/get-crawled-links) [])}
    "settings"
    (let [total (db/crawler-links-count)
          crawled (db/crawled-count)
          rss (db/rss-links-count)
          finished (db/finished-count)]
      {:crawler_links_count total
       :crawled_count crawled
       :pending_count (- total crawled)
       :rss_links_cout rss
       :commit_index false
       :rss_finished finished
       :rss_pending (- rss finished)
       :feeds_count (db/feeds-count)
       :fetcher_running (not (nil? @fetcher))
       :crawler_running (not (nil? @crawler))
       :black_domains (map (fn [p id] {:patten (str p)
                                      :id id})
                           @@conf/black-domain-pattens
                           (range))
       :reseted_domains (map (fn [p id] {:patten (str p)
                                        :id id})
                             @@conf/reseted-hosts
                             (range))})))

(defn settings [req]
  (let [data (-> req :body :_data)]
    (when (false? (:crawler_running data))
      (stop-crawler))
    (when (true? (:crawler_running data))
      (start-crawler))
    (when (false? (:fetcher_running data))
      (stop-fetcher))
    (when (true? (:fetcher_running data))
      (start-fetcher))
    nil))
