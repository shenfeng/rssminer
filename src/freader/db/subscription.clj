(ns freader.db.subscription
  (:use [freader.db.util :only [select-sql-params h2-insert-and-return
                                h2-query with-h2]]
        [clojure.java.jdbc :only [delete-rows update-values]]
        [freader.util :only [tracep]]
        [freader.search :only [index-feeds]]))

(defn insert [table data]
  (h2-insert-and-return table data))

(defn fetch-subscription [map]
  (first
   (h2-query (select-sql-params :rss_links map))))

(defn fetch-feeds-count-by-id [subscription-id]
  (-> (h2-query ["SELECT COUNT(*) as count
                FROM feeds WHERE rss_xml_id = ?" subscription-id])
      first :count))

(defn fetch-user-subscription [map]
  (first
   (h2-query
    (select-sql-params :user_subscriptions map))))

(defn fetch-categories [user-id feed-id]
  (h2-query ["SELECT type, text, added_ts FROM feedcategory
                WHERE user_id = ? AND
                      feed_id =?" user-id user-id]))

(defn fetch-comments [user-id feed-id]
  (h2-query ["SELECT id, content, added_ts FROM comments
                WHERE user_id = ? AND
                      feed_id = ? " user-id feed-id]))
(defn fetch-feeds
  ([subscription-id limit offset]
     (h2-query ["SELECT
                        id, author, title, summary, alternate, published_ts
                   FROM feeds
                   WHERE rss_link_id = ? LIMIT ? OFFSET ?"
                subscription-id limit offset])))

(defn fetch-overview [user-id]
  (h2-query ["
SELECT
   us.group_name, s.id, us.title, s.favicon,
   (SELECT COUNT(*) FROM feeds WHERE feeds.rss_link_id = s.id) AS total_count,
   (SELECT COUNT(*) FROM feeds
    WHERE  feeds.rss_link_id = s.id AND
           feeds.id NOT IN (SELECT feed_id FROM feedcategory
                             WHERE user_id = ? AND
                                  'type' = 'freader' AND
                                   text = 'read' )) AS unread_count
FROM
   user_subscriptions AS us
   JOIN rss_links AS s ON s.id = us.rss_link_id
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
                  {:rss_link_id (:id subscription)
                   :author (:author feed)
                   :title (:title feed)
                   :summary (-> feed :description :value)
                   :alternate (:link feed)
                   :published_ts (:publishedDate feed)})
          categories (:categories feed)]
      (index-feeds (list saved-feed))
      (doseq [c categories]
        (insert :feedcategory
                {:user_id user-id
                 :feed_id (:id saved-feed)
                 :type "tag"
                 :text (:name c)})))))

(defn update-user-subscription [user-id rss-link-id data]
  (with-h2
    (update-values :user_subscriptions
                   ["user_id = ? AND rss_link_id = ?" user-id rss-link-id]
                   (select-keys data [:group_name :title]))))

(defn delete-user-subscription [user-id rss-link-id]
  (with-h2
    (delete-rows :user_subscriptions
                 ["user_id = ? AND rss_link_id = ?" user-id rss-link-id])))
