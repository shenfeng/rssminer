(ns rssminer.db.feed
  (:use [rssminer.database :only [mysql-query with-mysql mysql-insert]]
        (rssminer [search :only [index-feed]]
                  [util :only [to-int now-seconds]]
                  [config :only [rssminer-conf]]
                  [classify :only [on-fetcher-event]])
        [clojure.string :only [blank?]]
        [clojure.tools.logging :only [warn]]
        [clojure.java.jdbc :only [do-prepared]])
  (:import rssminer.db.MinerDAO))

(defn update-total-feeds [rssid]
  (with-mysql (do-prepared "UPDATE rss_links SET total_feeds =
                 (SELECT COUNT(*) FROM feeds where rss_link_id = ?)
                 WHERE id = ?" [rssid rssid])))

(defn- feed-exits [rssid link]
  (mysql-query ["SELECT 1 FROM feeds WHERE rss_link_id = ? AND link = ?"
                rssid link]))

(defn- save-feed [feed rssid]
  (try (let [id (mysql-insert :feeds (dissoc (assoc feed :rss_link_id rssid)
                                             :summary))]
         (index-feed id rssid feed)
         (mysql-insert :feed_data {:id id :summary (:summary feed)})
         id)                            ; return id
       (catch Exception e
         (warn "insert for rss" rssid e))))

(defn save-feeds [feeds rssid]
  (let [ids (map (fn [{:keys [link] :as feed}]
                   (when (and link (not (blank? link)))
                     ;; link is the only cared,
                     (if-not (feed-exits rssid link)
                       (save-feed feed rssid))))
                 (:entries feeds))
        inserted (filter identity (doall ids))]
    (when (seq inserted)
      (on-fetcher-event rssid (map to-int inserted))
      (update-total-feeds rssid))))

(defn fetch-link [id]
  (:link (first (mysql-query ["SELECT link FROM feeds WHERE id = ?" id]))))

(defn fetch-feed [userid id]
  (rssminer.jsoup.HtmlUtils/compact
   (-> (mysql-query ["select summary from feed_data where id = ?"
                     id])
       first :summary) "http://shenfeng.me"))

(defn fetch-feed2 [userid id]
  (-> (mysql-query ["select summary from feed_data where id = ?"
                    id])
      first :summary))

(defn- get-rssid-by-feedid [id]
  (-> (mysql-query ["select rss_link_id from feeds where id = ?" id])
      first :rss_link_id))

;;; TODO. when autoCommit=false this complete,
;;; other threads does not see the change
(defn insert-user-vote [user-id feed-id vote]
  (let [now (now-seconds)
        rssid (get-rssid-by-feedid feed-id)]
    (with-mysql (do-prepared ;; rss_link_id default 0, which is ok
                 "INSERT INTO user_feed
                  (user_id, feed_id, rss_link_id, vote_user, vote_date) VALUES(?, ?, ?, ?, ?)
                 ON DUPLICATE KEY UPDATE vote_user = ?, vote_date = ?"
                 [user-id feed-id rssid vote now vote now]))))

(defn mark-as-read [user-id feed-id]
  (let [now (now-seconds)
        rssid (get-rssid-by-feedid feed-id)]
    (with-mysql (do-prepared ;; rss_link_id default 0
                 "INSERT INTO user_feed (user_id, feed_id, rss_link_id, read_date)
       VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE read_date = ?"
                 [user-id feed-id rssid now now]))))

(defn fetch-newest [userid limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (.fetchGNewest db userid limit offset)))

(defn fetch-likest [userid limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (.fetchGLikest db userid limit offset)))

(defn fetch-recent-read [userid limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (.fetchGRead db userid limit offset)))

(defn fetch-recent-vote [userid limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (.fetchGVote db userid limit offset)))

(defn fetch-sub-newest [userid subid limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (.fetchSubNewest db userid subid limit offset)))

(defn fetch-sub-oldest [userid subid limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (.fetchSubOldest db userid subid limit offset)))

(defn fetch-sub-likest [userid subid limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (.fetchSubLikest db userid subid limit offset)))

(defn fetch-sub-read [userid subid limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (.fetchSubRead db userid subid limit offset)))

(defn fetch-sub-vote [userid subid limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (.fetchSubVote db userid subid limit offset)))

(defn fetch-folder-newest [userid subids limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (.fetchFolderNewest db userid subids limit offset)))

(defn fetch-folder-oldest [userid subids limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (.fetchFolderOldest db userid subids limit offset)))

(defn fetch-folder-likest [userid subids limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (.fetchFolderLikest db userid subids limit offset)))

(defn fetch-folder-read [userid subids limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (.fetchFolderRead db userid subids limit offset)))

(defn fetch-folder-vote [userid subids limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (.fetchFolderVote db userid subids limit offset)))

