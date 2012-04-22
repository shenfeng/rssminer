(ns rssminer.handlers.dashboard
  (:require [rssminer.db.dashboard :as db]
            (rssminer [config :as conf]
                      [fetcher :as f]))
  (:import [rssminer.task HttpTaskRunner]))

(defn get-stat [req]
  {:rss_links (db/rss-links-count)
   :feeds (db/feeds-count)
   :fetcher (f/fetcher-stat)
   :fetcher_running (f/running?)})

(defn settings [req]
  (let [{:keys [which command]} (:body req)]
    (case which
      "fetcher"
      (if (= "start" command)
        (f/start-fetcher)
        (f/stop-fetcher)))
    nil))
