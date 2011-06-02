(ns feng.rss.db.feed
  (:use [feng.rss.db.util :only [exec-query select-sql-params
                                 insert-sql-params]]))

(defn inser-feedsource [feedsource]
  (first
   (exec-query (insert-sql-params :feedsources feedsource))))

(defn insert-user-feedsource [item]
  (first
   (exec-query (insert-sql-params :user_feedsource item))))

(defn insert-feed [feed]
  (first
   (exec-query (insert-sql-params :feeds feed))))

(defn insert-user-feed [item]
  (first
   (exec-query (insert-sql-params :user_feed item))))

(defn fetch-feedsource [map]
  (first
   (exec-query (select-sql-params :feedsources map))))

(defn fetch-user-feedsource [map]
  (exec-query
   (select-sql-params :user_feedsource map 100 0)))

(defn fetch-feeds
  ([fs-id] (exec-query ["SELECT * FROM feeds"]))
  ([user-id fs-id limit offset]
     (exec-query ["SELECT
                       feeds.*
                  FROM feeds
                       JOIN user_feed ON feeds.id = user_feed.feed_id
                  WHERE
                      user_feed.user_id = ? and feeds.feedsource_id = ?
                  LIMIT ? OFFSET ?"
                  user-id fs-id limit offset])))
