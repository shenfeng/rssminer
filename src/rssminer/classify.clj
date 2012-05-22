(ns rssminer.classify
  (:import rssminer.classfier.SysVoteDaemon))

(defonce daemon (atom nil))

(defn start-classify-daemon [datasource]
  (when (nil? @daemon)
    (reset! daemon (doto (SysVoteDaemon. datasource)
                     (.start)))))

(defn stop-classify-daemon []
  (when-not (nil? @daemon)
    (.stop ^SysVoteDaemon @daemon)))

(defn on-user-vote [user-id feed-id like]
  (when-not (nil? @daemon)
    (.onUserVote ^SysVoteDaemon @daemon user-id feed-id like)))
