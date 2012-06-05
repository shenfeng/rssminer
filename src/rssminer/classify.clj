(ns rssminer.classify
  (:use [rssminer.config :only [rssminer-conf]]
        [rssminer.util :only [to-int]])
  (:import rssminer.classfier.SysVoteDaemon))

(defonce daemon (atom nil))

(defn start-classify-daemon []
  (when (nil? @daemon)
    (reset! daemon (doto (SysVoteDaemon. @rssminer-conf)
                     (.start)))))

(defn stop-classify-daemon []
  (when-not (nil? @daemon)
    (.stop ^SysVoteDaemon @daemon)
    (reset! daemon nil)))

(defn on-fetcher-event [rssid feedids]
  (.onFecherEvent ^SysVoteDaemon @daemon rssid feedids))

(defn on-feed-event [userid feedid]
  (.onFeedEvent ^SysVoteDaemon @daemon userid feedid))
