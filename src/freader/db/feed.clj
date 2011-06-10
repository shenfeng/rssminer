(ns freader.db.feed
  (:use [freader.db.util :only [exec-query select-sql-params
                                 insert-sql-params]]))

(defn insert [table data]
  (first
   (exec-query (insert-sql-params table data))))

(defn fetch-subscription [map]
  (first
   (exec-query (select-sql-params :subscriptions map))))

(defn fetch-favicon-count [subscription-id]
  (first
   (exec-query ["SELECT favicon, (SELECT COUNT(*) FROM feeds
                                  WHERE subscription_id = ?) AS count
                FROM subscriptions WHERE id = ?"
                subscription-id subscription-id])))

(defn fetch-user-subscription [map]
  (exec-query
   (select-sql-params :user_subscription map 100 0)))

(defn fetch-categories [user-id feed-id]
  (exec-query ["SELECT type, text, added_ts FROM feedcategory
                WHERE user_id = ? AND
                      feed_id =?" user-id user-id]))

(defn fetch-comments [user-id feed-id]
  (exec-query ["SELECT id, content, added_ts FROM comments
                WHERE user_id = ? AND
                      feed_id = ? " user-id feed-id]))
(defn fetch-feeds
  ([subscription-id limit offset]
     (exec-query ["SELECT
                        id, author, title, summary, alternate, published_ts
                   FROM feeds
                   WHERE subscription_id = ? LIMIT ? OFFSET ?"
                  subscription-id limit offset])))

(defn fetch-overview [user-id]
  (exec-query ["
SELECT
   us.group_name, s.id, s.title, s.favicon,
   (SELECT COUNT(*) FROM feeds WHERE feeds.subscription_id = s.id) AS total_count,
   (SELECT COUNT(*) FROM feeds
    WHERE  feeds.subscription_id = s.id AND
           feeds.id NOT IN (SELECT feed_id FROM feedcategory
                             WHERE user_id = ? AND
                                  'type' = 'freader' AND
                                   text = 'read' )) AS unread_count
FROM
   user_subscription AS us
   JOIN subscriptions AS s ON s.id = us.subscription_id
WHERE us.user_id = ?" user-id user-id]))


