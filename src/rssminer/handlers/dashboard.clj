(ns rssminer.handlers.dashboard
  (:require [rssminer.db.dashboard :as db]
            (rssminer [config :as conf]
                      [fetcher :as f]
                      [crawler :as c]
                      [database :as d]))
  (:import [rssminer.task HttpTaskRunner]))

(defn get-data [req]
  (case (-> req :params :section)
    "rsslinks"
    {:caption "Newly added Rss Links"
     :data (or (db/get-rss-links) [])}
    "pending"
    {:caption "Pending Links"
     :data (or (db/get-pending-links) [])}
    "crawled"
    {:caption "Cawled Links"
     :data (or (db/get-crawled-links) [])}
    "stat"
    {:crawler_links (db/crawler-links-count)
     :rss_links (db/rss-links-count)
     :feeds (db/feeds-count)
     :crawler (c/crawler-stat)
     :crawler_running (c/running?)
     :fetcher (f/fetcher-stat)
     :fetcher_running (f/running?)
     :h2 (d/running?)}))

(defn settings [req]
  (let [{:keys [which command]} (:body req)]
    (case which
      "crawler"
      (if (= "start" command)
        (c/start-crawler)
        (c/stop-crawler))
      "fetcher"
      (if (= "start" command)
        (f/start-fetcher)
        (f/stop-fetcher))
      "h2"
      (if (= "start" command)
        (d/start-h2-server)
        (d/stop-h2-server)))
    nil))
