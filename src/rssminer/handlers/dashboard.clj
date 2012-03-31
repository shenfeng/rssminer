(ns rssminer.handlers.dashboard
  (:require [rssminer.db.dashboard :as db]
            (rssminer [config :as conf]
                      [fetcher :as f]
                      [crawler :as c]))
  (:import [rssminer.task HttpTaskRunner]))

(defn get-data [req]
  {:rss_links (db/rss-links-count)
   :feeds (db/feeds-count)
   :crawler (c/crawler-stat)
   :crawler_running (c/running?)
   :fetcher (f/fetcher-stat)
   :fetcher_running (f/running?)})

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
        (f/stop-fetcher)))
    nil))
