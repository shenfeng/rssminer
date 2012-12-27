(ns rssminer.classify
  (:use [rssminer.config :only [cfg]])
  (:import rssminer.classfier.SysVoteDaemon))

(defonce daemon (atom nil))

(defn running? [] @daemon)

(defn start-classify-daemon! []
  (when-not (running?)
    (reset! daemon (doto (SysVoteDaemon. (cfg :data-source)
                                         (cfg :redis-server)
                                         (cfg :events-threshold))
                     (.start)))))

(defn stop-classify-daemon! []
  (when (running?)
    (.stop ^SysVoteDaemon @daemon)
    (reset! daemon nil)))

(defn on-fetcher-event [rssid feedids]
  (when (running?)
    ;; this is delayed
    (.onFecherEvent ^SysVoteDaemon @daemon rssid feedids)))

;;; feedid == -1 => recompute
(defn on-feed-event [userid feedid]
  (when (running?)
    ;; computed now
    (.onFeedEvent ^SysVoteDaemon @daemon userid feedid)))
