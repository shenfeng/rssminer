(ns rssminer.db.feed
  (:use [rssminer.database :only [mysql-query with-mysql mysql-insert]]
        (rssminer [search :only [index-feed]]
                  [util :only [ignore-error to-int now-seconds]]
                  [classify :only [on-fetcher-event]])
        [clojure.string :only [blank?]]
        [clojure.tools.logging :only [info trace]]
        [clojure.java.jdbc :only [update-values delete-rows do-commands]]))

(defn- feed-exits [rssid link]
  (mysql-query ["SELECT id FROM feeds WHERE rss_link_id = ? AND link = ?"
                rssid link]))

(defn update-total-feeds [rssid]
  (with-mysql
    (do-commands (str "UPDATE rss_links SET total_feeds =
  (SELECT COUNT(*) FROM feeds where rss_link_id = " rssid ") WHERE id = "
  rssid))))

(defn save-feeds [feeds rssid]
  (let [ids (map (fn [{:keys [link] :as feed}]
                   (when (and link (not (blank? link)))
                     ;; since link is what I cared,
                     ;; do not update this feed on mysql
                     (if-not (feed-exits rssid link)
                       (let [id (mysql-insert :feeds (assoc feed
                                                       :rss_link_id rssid))]
                         (index-feed id rssid feed)
                         id))))        ; return id
                 (:entries feeds))
        inserted (filter identity (doall ids))]
    (when (seq inserted)
      (on-fetcher-event rssid (map to-int inserted))
      (update-total-feeds rssid))))

(defn fetch-link [id]
  (:link (first (mysql-query ["SELECT link FROM feeds WHERE id = ?" id]))))

(defn- safe-update-rss-link [id data]
  (with-mysql
    (update-values :rss_links ["id = ?" id] data)))

(defn update-rss-link [id data]
  (if-let [url (:url data)]
    (if-let [saved-id (-> (mysql-query
                           ["SELECT id FROM rss_links WHERE url = ?"
                            (:url data)]) first :id)]
      (do
        (with-mysql
          (update-values :user_subscription ["rss_link_id = ?" id]
                         {:rss_link_id saved-id})
          (update-values :feeds ["rss_link_id = ?" id]
                         {:rss_link_id saved-id})
          ;; (update-values :user_feed ["rss_link_id = ?" id]
          ;;                {:rss_link_id saved-id})
          (delete-rows :rss_links ["id = ?" id])))
      (safe-update-rss-link id data))
    (safe-update-rss-link id data)))

(defn update-feed [id data]
  (with-mysql
    (update-values :feeds ["id = ?" id] data)))

(defn fetch-rss-links [limit]           ; for fetcher
  "Returns nil when no more"
  (mysql-query ["SELECT id, url, check_interval, last_modified, etag
              FROM rss_links
              WHERE next_check_ts < ?
              ORDER BY next_check_ts LIMIT 0, ?" (now-seconds) limit]))

(defn fetch-rss-link [id]
  (first (mysql-query
          ["SELECT id, url, check_interval, last_modified, etag
              FROM rss_links where id = ?" id])))

(defn insert-rss-link [link]
  ;; ignore voilate of uniqe constraint
  (ignore-error (mysql-insert :rss_links link)))
