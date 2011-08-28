(ns rssminer.db.feed
  (:use [rssminer.database :only [h2-db-factory]]
        [rssminer.db.util :only [h2-query id-k with-h2 h2-insert]]
        [rssminer.search :only [index-feed]]
        [clojure.tools.logging :only [info]]
        [clojure.java.jdbc :only [with-connection with-query-results
                                  insert-record update-values]])
  (:import java.io.StringReader))

(defn fetch-tags [user-id feed-id]
  (map :tag (h2-query ["SELECT tag FROM feed_tag
              WHERE user_id = ? AND
              feed_id =?" user-id feed-id])))

(defn fetch-comments [user-id feed-id]
  (h2-query ["SELECT id, content, added_ts FROM comments
              WHERE user_id = ? AND
                    feed_id = ? " user-id feed-id]))

(defn insert-tags [feed-id user-id tags]
  (doseq [t tags]
    (with-h2 (insert-record :feed_tag  {:feed_id feed-id
                                        :user_id user-id
                                        :tag t}))))

(defn save-feeds [feeds rss-id user-id]
  (doseq [feed (:entries feeds)]
    (when (:guid feed)
      (try                              ; guid is uniqe
        (let [feed (dissoc (assoc feed :rss_link_id rss-id) :categories)
              feed-id (id-k (with-h2
                              (insert-record :feeds feed)))]
          (index-feed (assoc feed :id feed-id))
          (insert-tags feed-id user-id (:categories feed)))
        (catch RuntimeException e
          (info "update" (:guid feed))
          (with-connection @h2-db-factory
            (update-values :feeds ["guid=?" (:guid feed)]
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

(defn insert-rss-xml [xml]
  (h2-insert :rss_xmls
             {:content (StringReader. xml)
              :length (count xml)}))
