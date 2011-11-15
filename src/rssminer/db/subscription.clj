(ns rssminer.db.subscription
  (:use [rssminer.db.util :only [select-sql-params h2-insert-and-return
                                 h2-query with-h2]]
        [clojure.java.jdbc :only [delete-rows update-values]]))

(defn fetch-rss-link [map]
  (first
   (h2-query (select-sql-params :rss_links map))))

(defn fetch-feeds-count-by-id [rss-id]
  (-> (h2-query ["SELECT COUNT(*) as count
                FROM feeds WHERE rss_link_id = ?" rss-id])
      first :count))

(defn fetch-subs-by-user [user-id]
  (h2-query ["SELECT rss_link_id AS id, title
             FROM user_subscription WHERE user_id = ?" user-id]))

(defn fetch-subscription [map]
  (first
   (h2-query
    (select-sql-params :user_subscription map))))

(defn update-subscription [user-id id data]
  (with-h2
    (update-values :user_subscription
                   ["user_id = ? AND id = ?" user-id id]
                   (select-keys data [:group_name :title]))))

(defn delete-subscription [user-id id]
  (with-h2
    (delete-rows :user_subscription
                 ["user_id = ? AND id = ?" user-id id])))
