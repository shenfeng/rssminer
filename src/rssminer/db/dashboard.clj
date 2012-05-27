(ns rssminer.db.dashboard
  (:use [rssminer.db.util :only [mysql-query]]
        [rssminer.util :only [now-seconds]]))

(defn rss-links-count []
  (-> (mysql-query ["SELECT COUNT(*) as count FROM rss_links"])
      first :count))

(defn feeds-count []
  (-> (mysql-query ["SELECT COUNT(*) as count FROM feeds"])
      first :count))
