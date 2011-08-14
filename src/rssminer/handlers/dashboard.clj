(ns rssminer.handlers.dashboard
  (:require [rssminer.db.dashboard :as db]))

(defn- get-stats []
  {:total_count (db/get-total-count)
   :crawled_count (db/get-crawled-count)
   :rss_links_cout (db/get-rss-links-count)})

(defn get-rsslinks [req]
  (assoc (get-stats)
    :rss_links (or (db/get-rss-links) [])))

(defn get-crawler-pending [req]
  (assoc (get-stats)
    :pending_links (or (db/get-pending-links) [])))

(defn get-crawled [req]
  (assoc (get-stats)
    :crawled_links (or (db/get-crawled-links) [])))



