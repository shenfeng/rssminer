(ns rssminer.db.feed
  (:use [rssminer.database :only [mysql-query with-mysql mysql-insert]]
        (rssminer [search :only [index-feed]]
                  [redis :only [zrem]]
                  [util :only [to-int now-seconds ignore-error]]
                  [config :only [rssminer-conf]]
                  [classify :only [on-fetcher-event]])
        [clojure.string :only [blank?]]
        [clojure.tools.logging :only [warn]]
        [clojure.java.jdbc :only [do-prepared]])
  (:import rssminer.db.MinerDAO
           rssminer.Utils
           rssminer.jsoup.HtmlUtils))

(defn update-total-feeds [rssid]
  (with-mysql (do-prepared "UPDATE rss_links SET total_feeds =
                 (SELECT COUNT(*) FROM feeds where rss_link_id = ?)
                 WHERE id = ?" [rssid rssid])))

(defn- feed-exits? [rssid link]
  (mysql-query
   ["SELECT 1 FROM feeds WHERE rss_link_id = ? AND link_hash = ? AND link = ?"
    rssid (.hashCode ^String link) link]))

(defn- save-feed [feed rssid]
  (try (let [id (mysql-insert :feeds (dissoc (assoc feed :rss_link_id rssid)
                                             :summary))
             ;; always compact html to save disk storage
             feed (assoc feed :summary
                         (HtmlUtils/compact (:summary feed)
                                            (:link feed)))]
         (index-feed id rssid feed)
         (mysql-insert :feed_data {:id id :summary (:summary feed)})
         id)                            ; return id
       (catch Exception e
         (warn "insert for rss" rssid e))))

(defn save-feeds [feeds rssid]
  (let [ids (map (fn [{:keys [link] :as feed}]
                   (when (and link (not (blank? link)))
                     ;; link is the only cared,
                     (if-not (feed-exits? rssid link)
                       (save-feed (assoc feed
                                    :link_hash (.hashCode ^String link))
                                  rssid))))
                 (:entries feeds))
        inserted (filter identity (doall ids))]
    (when (seq inserted)
      (on-fetcher-event rssid (map to-int inserted))
      (update-total-feeds rssid))))

(defn fetch-link [id]
  (:link (first (mysql-query ["SELECT link FROM feeds WHERE id = ?" id]))))

(defn fetch-feeds [userid ids]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (.fetchFeedsWithSummary db userid ids)))

(defn- get-rssid-by-feedid [id]
  (-> (mysql-query ["select rss_link_id from feeds where id = ?" id])
      first :rss_link_id))

;;; TODO. when autoCommit=false this complete,
;;; other threads does not see the change
(defn insert-user-vote [user-id feed-id vote]
  (let [now (now-seconds)
        rssid (get-rssid-by-feedid feed-id)]
    (when rssid
      (with-mysql (do-prepared ;; rss_link_id default 0, which is ok
                   "INSERT INTO user_feed
                  (user_id, feed_id, rss_link_id, vote_user, vote_date) VALUES(?, ?, ?, ?, ?)
                 ON DUPLICATE KEY UPDATE vote_user = ?, vote_date = ?"
                   [user-id feed-id rssid vote now vote now])))))

(defn mark-as-read [user-id feed-id]
  (let [now (now-seconds)
        rssid (get-rssid-by-feedid feed-id)]
    (when rssid
      (with-mysql (do-prepared ;; rss_link_id default 0
                   "INSERT INTO user_feed (user_id, feed_id, rss_link_id, read_date)
       VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE read_date = ?"
                   [user-id feed-id rssid now now]))
      (zrem (Utils/genKey user-id rssid) (str feed-id)))))

(def update-sql ["update user_feed set read_time = read_time + ? where user_id = ? and feed_id = ?"])

;; data is a map {:feed_id time}
(defn update-reading-time [user-id data]
  (ignore-error                         ; read_time may out of range, MEDIUMINT UNSIGNED
   (with-mysql
     (apply do-prepared
            (concat update-sql
                    (map (fn [[feedid time]]
                           ;; max 5 minutes, time in 0.1s
                           [(min (* 10 60 5) time)
                            user-id (to-int (name feedid))]) data))))))

(defn- dedup [feeds]
  {:count (count feeds)
   :feeds (MinerDAO/removeDuplicate feeds)})

(defn fetch-newest [userid limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (dedup (.fetchGNewest db userid limit offset))))

(defn fetch-likest [userid limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (dedup (.fetchGLikest db userid limit offset))))

(defn fetch-recent-read [userid limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (dedup (.fetchGRead db userid limit offset))))

(defn fetch-recent-vote [userid limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (dedup (.fetchGVote db userid limit offset))))

(defn fetch-sub-newest [userid subid limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (dedup (.fetchSubNewest db userid subid limit offset))))

(defn fetch-sub-oldest [userid subid limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (dedup (.fetchSubOldest db userid subid limit offset))))

(defn fetch-sub-likest [userid subid limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (dedup (.fetchSubLikest db userid subid limit offset))))

(defn fetch-sub-read [userid subid limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (dedup (.fetchSubRead db userid subid limit offset))))

(defn fetch-sub-vote [userid subid limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (dedup (.fetchSubVote db userid subid limit offset))))

(defn fetch-folder-newest [userid subids limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (dedup (.fetchFolderNewest db userid subids limit offset))))

(defn fetch-folder-oldest [userid subids limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (dedup (.fetchFolderOldest db userid subids limit offset))))

(defn fetch-folder-likest [userid subids limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (dedup (.fetchFolderLikest db userid subids limit offset))))

(defn fetch-folder-read [userid subids limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (dedup (.fetchFolderRead db userid subids limit offset))))

(defn fetch-folder-vote [userid subids limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (dedup (.fetchFolderVote db userid subids limit offset))))
