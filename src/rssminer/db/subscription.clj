(ns rssminer.db.subscription
  (:use [rssminer.db.util :only [select-sql-params mysql-insert-and-return
                                 mysql-query with-mysql]]
        [clojure.java.jdbc :only [delete-rows update-values]]))

(defn fetch-rss-link [map]
  (first
   (mysql-query (select-sql-params :rss_links map))))

(defn fetch-feeds-count-by-id [rss-id]
  (-> (mysql-query ["SELECT COUNT(*) as count
                FROM feeds WHERE rss_link_id = ?" rss-id])
      first :count))

(defn fetch-user-subs [user-id mark-as-read-time like neutral]
  (mysql-query ["call get_user_subs(?, ?, ?, ?)"
                user-id mark-as-read-time like neutral]))

(defn fetch-user-sub [id user-id mark-as-read-time like neutral]
  (mysql-query ["SELECT rl.id, rl.url, rl.title
              FROM rss_links rl WHERE id = ?" id]))

(defn fetch-subscription [map]
  (first
   (mysql-query
    (select-sql-params :user_subscription map))))

(defn update-subscription [user-id id data]
  (with-mysql
    (update-values :user_subscription
                   ["user_id = ? AND id = ?" user-id id]
                   (select-keys data [:group_name :title]))))

(defn delete-subscription [user-id id]
  (with-mysql
    (delete-rows :user_subscription
                 ["user_id = ? AND id = ?" user-id id])))
