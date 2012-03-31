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
  (mysql-query ["SELECT us.rss_link_id AS id, us.group_name, l.url,
              us.sort_index, us.title, l.title as o_title,
           (SELECT COUNT(*) FROM feeds
              LEFT JOIN user_feed ON feeds.id = user_feed.feed_id
              WHERE rss_link_id = us.rss_link_id
              AND published_ts > ? AND
              (user_feed.read_date < 1 OR user_feed.read_date IS NULL))
              AS total_c,
           (SELECT COUNT(*) FROM feeds
              LEFT JOIN user_feed ON feeds.id = user_feed.feed_id
              WHERE rss_link_id = us.rss_link_id
              AND published_ts > ? AND user_feed.read_date < 1
              AND user_feed.vote_sys > ?) AS like_c,
           (SELECT COUNT(*) FROM feeds
              LEFT JOIN user_feed ON feeds.id = user_feed.feed_id
              WHERE rss_link_id = us.rss_link_id
              AND published_ts > ? AND user_feed.read_date < 1
              AND user_feed.vote_sys < ?) AS dislike_c
           FROM user_subscription us JOIN rss_links l
            ON l.id = us.rss_link_id WHERE us.user_id = ?"
             mark-as-read-time mark-as-read-time
             like mark-as-read-time neutral user-id]))

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
