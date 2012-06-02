(ns rssminer.db.user-feed
  (:use [rssminer.database :only [mysql-query with-mysql]]
        [rssminer.util :only [now-seconds]]
        [rssminer.config :only [rssminer-conf]]
        [clojure.java.jdbc :only [do-commands]])
  (:import rssminer.db.MinerDAO))

;;; vote time is not recoreded
(defn insert-user-vote [user-id feed-id vote]
  (let [n (now-seconds)]
    (with-mysql (do-commands ;; rss_link_id default 0, which is ok
                 (format "INSERT INTO user_feed
                      (user_id, feed_id, vote_user, vote_date)
             VALUES (%d, %d, %d, %d) ON DUPLICATE KEY
             UPDATE vote_user = %d, vote_date = %d"
                         user-id feed-id vote n vote n)))))

(defn mark-as-read [user-id feed-id]
  (let [now (now-seconds)]
    (with-mysql (do-commands ;; rss_link_id default 0
                 (format "INSERT INTO user_feed (user_id, feed_id, read_date)
       VALUES (%d, %d, %d) ON DUPLICATE KEY UPDATE read_date = %d"
                         user-id feed-id now now)))))

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

(defn fetch-sub-newest [userid, subid, limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (.fetchSubNewest db userid subid limit offset)))

(defn fetch-sub-oldest [userid, subid, limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (.fetchSubOldest db userid subid limit offset)))

(defn fetch-sub-likest [userid, subid, limit offset]
  (let [^MinerDAO db (MinerDAO. @rssminer-conf)]
    (.fetchSubLikest db userid subid limit offset)))
