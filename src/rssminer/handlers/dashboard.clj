(ns rssminer.handlers.dashboard
  (:use (rssminer [fetcher :only [start-fetcher stop-fetcher fetcher]]
                  [crawler :only [start-crawler stop-crawler
                                  running? crawler-stat]]))
  (:require [rssminer.db.dashboard :as db]
            [rssminer.config :as conf])
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
    {:crawler_links_count (db/crawler-links-count)
     :rss_links_cout (db/rss-links-count)
     :feeds_count (db/feeds-count)
     :crawler_running (running?)
     :crawler (crawler-stat)
     :fetcher_running (not (nil? @fetcher))
     :black_domains (map (fn [p id] {:patten (str p)
                                    :id id})
                         @@conf/black-domain-pattens (range))
     :reseted_domains (map (fn [p id] {:patten (str p)
                                      :id id})
                           @@conf/reseted-hosts (range))}))

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
