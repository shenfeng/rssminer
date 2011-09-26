(ns rssminer.db.feed
  (:use [rssminer.database :only [h2-db-factory]]
        [rssminer.db.util :only [h2-query id-k with-h2 h2-insert]]
        [rssminer.search :only [index-feed]]
        [rssminer.util :only [ignore-error]]
        [clojure.string :only [blank?]]
        [clojure.tools.logging :only [info]]
        [clojure.java.jdbc :only [with-connection insert-record
                                  update-values]])
  (:import java.io.StringReader))

(defn fetch-tags [user-id feed-id]
  (map :tag (h2-query ["SELECT tag FROM feed_tag
                        WHERE user_id = ? AND feed_id =?"
                       user-id feed-id])))

(defn fetch-comments [user-id feed-id]
  (h2-query ["SELECT id, content, added_ts FROM comments
              WHERE user_id = ? AND feed_id = ? " user-id feed-id]))

(defn insert-tags [feed-id user-id tags]
  (doseq [t tags]
    (ignore-error
     (with-h2 (insert-record :feed_tag  {:feed_id feed-id
                                         :user_id user-id
                                         :tag t})))))

(defn insert-pref [user-id feed-id pref]
  (with-h2
    (try (insert-record :user_feed_pref {:user_id user-id
                                         :feed_id feed-id
                                         :pref pref})
         (catch Exception e             ;unique key use_id & feed_id
           (update-values :user_feed_pref
                          ["user_id = ? AND feed_id = ?" user-id feed-id]
                          {:pref pref})))))

(defn save-feeds [feeds rss-id user-id]
  (doseq [{:keys [link categories] :as feed} (:entries feeds)]
    (when (and link (not (blank? link)))
      (try
        (let [feed (dissoc (assoc feed :rss_link_id rss-id) :categories)
              feed-id (id-k (with-h2
                              (insert-record :feeds feed)))]
          (index-feed (assoc feed :id feed-id) categories)
          (insert-tags feed-id user-id categories))
        (catch RuntimeException e       ;link is uniqe
          (info "update" link)
          (with-connection @h2-db-factory
            (update-values :feeds ["link=?" link]
                           (dissoc feed :categories))))))))

(defn fetch-feeds [rss-link-id limit offset]
  (h2-query ["SELECT id, author, title, summary, link, published_ts
              FROM feeds
              WHERE rss_link_id = ? LIMIT ? OFFSET ?"
             rss-link-id limit offset]))

(defn fetch-latest-feed [limit]
  (h2-query ["SELECT id, author, title, summary, link FROM feeds
              WHERE id > (SELECT MAX(id) FROM feeds) - ?
              ORDER BY id DESC LIMIT ?" (* 2 limit ) limit]))

(defn fetch-feeds-for-user
  ([user-id rss-id]
     (fetch-feeds-for-user user-id rss-id 20 0))
  ([user-id rss-id limit offset]
     (map #(assoc %
             :comments (or (fetch-comments user-id (:id %)) [])
             :tags (or (fetch-tags user-id (:id %)) []))
          (fetch-feeds rss-id limit offset))))
