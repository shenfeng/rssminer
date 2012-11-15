(ns rssminer.classify
  (:use [rssminer.config :only [cfg]])
  (:import rssminer.classfier.SysVoteDaemon))

(defonce daemon (atom nil))

(defn start-classify-daemon! []
  (when (nil? @daemon)
    (reset! daemon (doto (SysVoteDaemon. (cfg :data-source)
                                         (cfg :redis-server)
                                         (cfg :events-threshold))
                     (.start)))))

(defn stop-classify-daemon! []
  (when-not (nil? @daemon)
    (.stop ^SysVoteDaemon @daemon)
    (reset! daemon nil)))

(defn on-fetcher-event [rssid feedids]
  (when-not (nil? @daemon)
    ;; this is delayed
    (.onFecherEvent ^SysVoteDaemon @daemon rssid feedids)))

(defn on-feed-event [userid feedid]
  (when-not (nil? @daemon)
    ;; computed now
    (.onFeedEvent ^SysVoteDaemon @daemon userid feedid)))
