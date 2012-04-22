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

(defn fetch-subscription [user-id rss-link-id]
  (first (mysql-query ["SELECT id, rss_link_id, title, group_name FROM
                       user_subscription
                       WHERE user_id = ? AND rss_link_id = ?"
                       user-id rss-link-id])))

(defn update-subscription [user-id id data]
  (with-mysql
    (update-values :user_subscription
                   ["user_id = ? AND id = ?" user-id id]
                   (select-keys data [:group_name :title]))))

(defn fetch-user-subsurls [user-id]
  (mysql-query ["SELECT url FROM rss_links r JOIN user_subscription s
                 ON r.id = s.rss_link_id WHERE s.user_id = ?" user-id]))

(defn delete-subscription [user-id id]
  (with-mysql
    (delete-rows :user_subscription
                 ["user_id = ? AND id = ?" user-id id])))
