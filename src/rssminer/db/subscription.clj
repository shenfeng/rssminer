(ns rssminer.db.subscription
  (:use [rssminer.db.util :only [select-sql-params h2-insert-and-return
                                 h2-query with-h2]]
        [clojure.java.jdbc :only [delete-rows update-values]]
        [rssminer.util :only [tracep]])
  (:require [rssminer.db.feed :as fdb]))

(defn fetch-rss-link [map]
  (first
   (h2-query (select-sql-params :rss_links map))))

(defn fetch-feeds-count-by-id [rss-id]
  (-> (h2-query ["SELECT COUNT(*) as count
                FROM feeds WHERE rss_link_id = ?" rss-id])
      first :count))

(defn fetch-subscription [map]
  (first
   (h2-query
    (select-sql-params :user_subscription map))))

(defn fetch-overview [user-id]
  (h2-query ["
SELECT
   us.group_name, s.id, us.title, s.favicon,
   (SELECT COUNT(*) FROM feeds WHERE feeds.rss_link_id = s.id) AS total_count,
   (SELECT COUNT(*) FROM feeds
    WHERE  feeds.rss_link_id = s.id AND
           feeds.id NOT IN (SELECT feed_id FROM feed_tag
                             WHERE user_id is null AND
                                  tag = '_read')) AS unread_count
FROM
   user_subscription AS us
   JOIN rss_links AS s ON s.id = us.rss_link_id
WHERE us.user_id = ?" user-id]))

(defn update-subscription [user-id id data]
  (with-h2
    (update-values :user_subscription
                   ["user_id = ? AND id = ?" user-id id]
                   (select-keys data [:group_name :title]))))

(defn delete-subscription [user-id id]
  (with-h2
    (delete-rows :user_subscription
                 ["user_id = ? AND id = ?" user-id id])))
