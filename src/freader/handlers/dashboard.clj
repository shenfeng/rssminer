(ns freader.handlers.dashboard
  (:require [freader.db.dashboard :as db]))

(defn get-crawler-stats [req]
  {:total_count (db/get-total-count)
   :crawled_count (db/get-crawled-count)
   :crawled_links (or (db/get-crawled-links) [])
   :pending_links (or (db/get-pending-links) [])
   :rss_links (or (db/get-rss-links) [])})
