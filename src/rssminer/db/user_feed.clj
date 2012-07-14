(ns rssminer.db.user-feed
  (:use [rssminer.database :only [mysql-query with-mysql]]
        [rssminer.util :only [now-seconds]]
        [rssminer.config :only [rssminer-conf]]
        [clojure.java.jdbc :only [do-prepared]])
  (:import rssminer.db.MinerDAO))

;;; TODO. when autoCommit=false this complete,
;;; other threads does not see the change
(defn insert-user-vote [user-id feed-id vote]
  (let [now (now-seconds)]
    (with-mysql (do-prepared ;; rss_link_id default 0, which is ok
                 "INSERT INTO user_feed
                  (user_id, feed_id, vote_user, vote_date) VALUES(?, ?, ?, ?)
                 ON DUPLICATE KEY UPDATE vote_user = ?, vote_date = ?"
                 [user-id feed-id vote now vote now]))))

(defn mark-as-read [user-id feed-id]
  (let [now (now-seconds)]
    (with-mysql (do-prepared ;; rss_link_id default 0
                 "INSERT INTO user_feed (user_id, feed_id, read_date)
       VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE read_date = ?"
                 [user-id feed-id now now]))))

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
