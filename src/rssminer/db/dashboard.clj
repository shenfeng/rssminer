(ns rssminer.db.dashboard
  (:use [rssminer.db.util :only [h2-query]]
        [rssminer.time :only [now-seconds]]))

(defn crawled-count []
  (-> (h2-query ["SELECT COUNT (*) as count FROM crawler_links
                  WHERE next_check_ts > ?" (now-seconds)]) first :count))

(defn rss-links-count []
  (-> (h2-query ["SELECT COUNT (*) as count FROM rss_links"])
      first :count))

(defn crawler-links-count []
  (-> (h2-query ["SELECT COUNT(*) as count FROM crawler_links"])
      first :count))

(defn feeds-count []
  (-> (h2-query ["SELECT COUNT(*) as count FROM feeds"])
      first :count))
