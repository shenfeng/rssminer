(ns freader.db.subscription
  (:use [freader.db.util :only [exec-query select-sql-params
                                insert-sql-params]]))

(defn insert [table data]
  (first
   (exec-query (insert-sql-params table data))))

(defn fetch-subscription [map]
  (first
   (exec-query (select-sql-params :subscriptions map))))

(defn fetch-feeds-count-by-id [subscription-id]
  (-> (exec-query ["SELECT COUNT(*) as count
                FROM feeds WHERE subscription_id = ?" subscription-id])
      first :count))

(defn fetch-user-subscription [map]
  (first
   (exec-query
    (select-sql-params :user_subscription map))))

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
   us.group_name, s.id, us.title, s.favicon,
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


(defn fetch-feeds-by-subscription-id
  ([user-id subscription-id]
     (fetch-feeds-by-subscription-id user-id subscription-id 20 0))
  ([user-id subscription-id limit offset]
     (let [subscription (fetch-subscription {:id subscription-id})
           feeds (fetch-feeds (:id subscription) limit offset)
           items (map
                  (fn [f]
                    (assoc f
                      :comments (or
                                 (fetch-comments user-id (:id f)) [])
                      :categories (or
                                   (fetch-categories user-id (:id f)) [])))
                  feeds)]
       {:id subscription-id
        :title (:title subscription)
        :description (:description subscription)
        :alternate (:alternate subscription)
        :updated_ts (:updated_ts subscription)
        :continuation (when (= limit (count feeds)) (+ offset limit))
        :items items})))

(defn save-feeds [subscription feeds user-id]
  (doseq [feed (:entries feeds)]
    (let [saved-feed
          (insert :feeds
                  {:subscription_id (:id subscription)
                   :author (:author feed)
                   :title (:title feed)
                   :summary (-> feed :description :value)
                   :alternate (:link feed)
                   :published_ts (:publishedDate feed)})
          categories (:categories feed)]
      (doseq [c categories]
        (insert :feedcategory
                {:user_id user-id
                 :feed_id (:id saved-feed)
                 :type "tag"
                 :text (:name c)})))))

(defn update-user-subscription [user-id subscription-id data]
  (first
   (exec-query ["UPDATE user_subscription
               SET group_name = ?, title = ?
               WHERE user_id = ? AND subscription_id = ?
               RETURNING group_name, title"
                (:group_name data) (:title data) user-id subscription-id])))

(defn delete-user-subscription [user-id subscription-id]
  (first
   (exec-query ["DELETE FROM user_subscription
                 WHERE user_id = ? AND
                 subscription_id = ? RETURNING *"
                user-id subscription-id])))
