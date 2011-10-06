(ns rssminer.handlers.dashboard
  (:require [rssminer.db.dashboard :as db]
            (rssminer [config :as conf]
                      [fetcher :as f]
                      [crawler :as c]))
  (:import [rssminer.task HttpTaskRunner]))

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
    {:crawler_links (db/crawler-links-count)
     :rss_links (db/rss-links-count)
     :feeds (db/feeds-count)
     :crawler_running (c/running?)
     :crawler (c/crawler-stat)
     :fetcher (f/fetcher-stat)
     :fetcher_running (f/running?)}))

(defn settings [req]
  (let [data (-> req :body :_data)]
    (when (false? (:crawler_running data))
      (c/stop-crawler))
    (when (true? (:crawler_running data))
      (c/start-crawler))
    (when (false? (:fetcher_running data))
      (f/stop-fetcher))
    (when (true? (:fetcher_running data))
      (f/start-fetcher))
    nil))
