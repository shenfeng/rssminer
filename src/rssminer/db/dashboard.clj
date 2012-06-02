(ns rssminer.db.dashboard
  (:use [rssminer.database :only [mysql-query]]
        [rssminer.util :only [now-seconds]]))

(defn rss-links-count []
  (-> (mysql-query ["SELECT COUNT(*) as count FROM rss_links"])
      first :count))

(defn feeds-count []
  (-> (mysql-query ["SELECT COUNT(*) as count FROM feeds"])
      first :count))
