(ns rssminer.db.subscription
  (:use [rssminer.database :only [mysql-insert-and-return
                                  mysql-query with-mysql]]
        [rssminer.util :only [now-seconds]]
        [rssminer.config :only [cfg]]
        [clojure.java.jdbc :only [delete-rows update-values do-commands]])
  (:import rssminer.db.MinerDAO))

(defn- nil-fill [data key]
  (if (contains? data key)
    data
    (assoc data key nil)))

(defn fetch-rss-link-by-url [url]
  (first (mysql-query
          ["SELECT * FROM rss_links where url = ?" url])))

(defn fetch-rss-link-by-id [id]
  (first (mysql-query
          ["SELECT id, url, check_interval, last_modified, etag
              FROM rss_links where id = ?" id])))

(defn fetch-rss-links [limit]           ; for fetcher
  "Returns nil when no more"
  (mysql-query ["SELECT id, url, check_interval, last_modified, etag
              FROM rss_links
              WHERE next_check_ts < ?
              ORDER BY next_check_ts LIMIT 0, ?" (now-seconds) limit]))

;;; make sure last_modified and :etag are always updated
(defn- safe-update-rss-link [id data]
  (let [data (nil-fill (nil-fill data :last_modified) :etag)]
    (with-mysql
      (update-values :rss_links ["id = ?" id] data))))

(defn update-rss-link [id data]
  (if-let [url (:url data)]
    (if-let [sid (-> (fetch-rss-link-by-url (:url data)) :id)]
      (let [old (mysql-query ["SELECT id FROM user_subscription WHERE
                               rss_link_id = ?" id])]
        (doseq [id old]
          (with-mysql
            (try (update-values :user_subscription ["id = ?" (:id id)]
                                {:rss_link_id sid})
                 (catch Exception e      ;duplicate, already subscribed
                   (delete-rows :user_subscription ["id=?" (:id id)])))))
        (with-mysql
          (delete-rows :feeds ["rss_link_id = ?" id])
          (delete-rows :rss_links ["id = ?" id])))
      (safe-update-rss-link id data))   ;no saved, just update this one
    (safe-update-rss-link id data)))

(defn fetch-feeds-count-by-id [rss-id]
  (-> (mysql-query ["SELECT COUNT(*) as count
                FROM feeds WHERE rss_link_id = ?" rss-id])
      first :count))

(defn fetch-subscription [user-id rss-link-id]
  (first (mysql-query ["SELECT id, rss_link_id, title, group_name FROM
                       user_subscription
                       WHERE user_id = ? AND rss_link_id = ?"
                       user-id rss-link-id])))

(defn delete-subscription [user-id rss-id]
  (with-mysql
    (delete-rows :user_subscription
                 ["user_id = ? AND rss_link_id = ?" user-id rss-id])
    (delete-rows :user_feed
                 ["user_id = ? AND rss_link_id = ?" user-id rss-id])))

(defn update-sort-order [user-id data]
  (with-mysql
    (apply do-commands
           (map (fn [d idx] (str "UPDATE user_subscription SET sort_index = "
                                idx
                                " , group_name = '" (:g d) "'"
                                " WHERE user_id = " user-id
                                " AND rss_link_id = " (:id d)))
                data (range 400 200000 4)))))

(defn fetch-user-sub [userid id]
  (let [^MinerDAO db (MinerDAO. (cfg :data-source) (cfg :redis-server))]
    (.fetchUserSub db userid id)))

(defn fetch-user-subs [userid]
  (let [^MinerDAO db (MinerDAO. (cfg :data-source) (cfg :redis-server))]
    (.fetchUserSubs db userid)))
