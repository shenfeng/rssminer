(ns rssminer.db.feed
  (:use [rssminer.db.util :only [h2-query id-k with-h2 h2-insert]]
        (rssminer [database :only [h2-db-factory]]
                  [search :only [index-feed]]
                  [time :only [now-seconds]]
                  [util :only [ignore-error extract-text gen-snippet]])
        [clojure.string :only [blank?]]
        [clojure.tools.logging :only [info trace]]
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

(defn insert-pref [user-id feed-id pref]
  (with-h2
    (try (insert-record :user_feed {:user_id user-id
                                    :feed_id feed-id
                                    :pref pref})
         (catch Exception e             ;unique key use_id & feed_id
           (update-values :user_feed
                          ["user_id = ? AND feed_id = ?" user-id feed-id]
                          {:pref pref})))))

(defn insert-tags [feed-id user-id tags]
  (doseq [t tags]
    (ignore-error
     (with-h2 (insert-record :feed_tag  {:feed_id feed-id
                                         :user_id user-id
                                         :tag t})))))

(defn save-feeds [feeds rss-id]
  (doseq [{:keys [link tags] :as feed} (:entries feeds)]
    (when (and link (not (blank? link)))
      (try
        (let [content (extract-text (:summary feed))
              snippet (gen-snippet content)
              id (id-k (with-h2
                         (insert-record :feeds
                                        (assoc (dissoc feed :tags)
                                          :rss_link_id rss-id
                                          :snippet snippet))))]
          (doseq [t tags]
            (ignore-error
             (with-h2 (insert-record :feed_tag  {:feed_id id
                                                 :tag t}))))
          (index-feed id content feed))
        (catch RuntimeException e       ;link is uniqe
          (trace "update" link)
          (with-connection @h2-db-factory
            (update-values :feeds ["link=?" link] (dissoc feed :tags))))))))

(defn fetch-feeds [rss-link-id limit offset]
  (h2-query ["SELECT id, author, title, summary, link, published_ts
              FROM feeds
              WHERE rss_link_id = ? LIMIT ? OFFSET ?"
             rss-link-id limit offset] :convert))

(defn fetch-unread-meta [user-id]
  (h2-query ["SELECT c.* FROM (
       SELECT f.id as f_id, us.rss_link_id, f.published_ts FROM
       feeds f JOIN user_subscription us ON us.rss_link_id = f.rss_link_id
       WHERE us.user_id = ? ) AS c
       LEFT OUTER JOIN user_feed uf ON uf.feed_id = c.f_id
       WHERE (uf.read = FALSE OR uf.read IS NULL)" user-id]))

(defn fetch-unread-count-by-tag [feed-ids]
  (h2-query ["SELECT tag t, count(tag) c FROM
              TABLE(x int=?) T INNER JOIN feed_tag ft ON T.x = ft.feed_id
              GROUP BY tag" (into-array feed-ids)]))

(defn fetch-latest-feed [limit]
  (h2-query ["SELECT id, author, title, summary, link FROM feeds
              WHERE id > (SELECT MAX(id) FROM feeds) - ?
              ORDER BY id DESC LIMIT ?" (* 2 limit ) limit] :convert))

(defn fetch-feeds-for-user
  ([user-id rss-id]
     (fetch-feeds-for-user user-id rss-id 20 0))
  ([user-id rss-id limit offset]
     (map #(assoc %
             :comments (or (fetch-comments user-id (:id %)) [])
             :tags (or (fetch-tags user-id (:id %)) []))
          (fetch-feeds rss-id limit offset))))

(defn update-rss-link [id data]
  (with-h2
    (update-values :rss_links ["id = ?" id] data)))

(defn fetch-rss-links [limit]
  "Returns nil when no more"
  (h2-query ["SELECT id, url, check_interval, last_modified
              FROM rss_links
              WHERE next_check_ts < ?
              ORDER BY next_check_ts LIMIT ?" (now-seconds) limit]))

(defn insert-rss-link
  [link]
  (ignore-error ;; ignore voilate of uniqe constraint
   (with-h2
     (insert-record :rss_links link))))
