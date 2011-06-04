(ns feng.rss.db.feed
  (:use [feng.rss.db.util :only [exec-query select-sql-params
                                 insert-sql-params]]))

(defn insert [table data]
  (first
   (exec-query (insert-sql-params table data))))

(defn fetch-subscription [map]
  (first
   (exec-query (select-sql-params :subscriptions map))))

(defn fetch-user-subscription [map]
  (exec-query
   (select-sql-params :user_subscription map 100 0)))

(defn fetch-feeds
  ([subscription-id limit offset]
     (exec-query ["SELECT
                        *
                   FROM feeds
                   WHERE subscription_id = ? LIMIT ? OFFSET ?"
                  subscription-id limit offset])))

