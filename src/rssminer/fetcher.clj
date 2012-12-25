(ns rssminer.fetcher
  (:use [clojure.tools.logging :only [warn info error]]
        (rssminer [util :only [assoc-if now-seconds]]
                  [parser :only [parse-feed]]
                  [redis :only [fetcher-dequeue fetcher-enqueue]]
                  [config :only [rssminer-conf]]))
  (:require [rssminer.db.feed :as db]
            [rssminer.db.subscription :as subdb])
  (:import [rssminer.fetcher HttpTaskRunner IHttpTask IHttpTasksProvder
            HttpTaskRunnerConf IBlockingTaskProvider]
           me.shenfeng.http.HttpUtils))

(defonce fetcher (atom nil))

(defn running? []
  (if-not (nil? @fetcher)
    (.isRunning ^HttpTaskRunner @fetcher)
    false))

(defn stop-fetcher []
  (when (running?)
    (.stop ^HttpTaskRunner @fetcher)))

(defn fetcher-stat []
  (when-not (nil? @fetcher)
    (.getStat ^HttpTaskRunner @fetcher)))

(defn- slower [last-interval]
  (min (int (* last-interval 1.4)) (* 3600 24 3))) ;3 days

(defn- quicker [last-interval]
  (max (* 3600 8) (int (/ last-interval 1.4)))) ; min 8h

;; TODO better last_modified and etag policy
(defn- next-check [last-interval status headers]
  (if-let [location (get headers HttpUtils/LOCATION)]
    {:url location :next_check_ts (rand-int 100000)}
    (let [interval (if (= 200 status)
                     (quicker last-interval) (slower last-interval))]
      {:check_interval interval
       :last_modified (get headers HttpUtils/LAST_MODIFIED)
       :etag (if-let [^String etag (get headers HttpUtils/ETAG)]
               (when (< (.length etag) 64)
                 etag))
       :next_check_ts (+ (now-seconds) interval
                         ;; to seperate them out, 30 minutes
                         (- 3600 (rand-int 7200)))})))

(defn handle-resp [{:keys [id url check_interval last_modified url]}
                   status headers body]
  (let [feeds (when (and (= 200 status) body) (parse-feed body))
        updated (assoc-if (next-check check_interval status headers)
                          :alternate (:link feeds)
                          :last_status status
                          :error_msg ""
                          :description (:description feeds)
                          :title (:title feeds))]
    ;; (info (str "id:" id) status url
    ;;       (str "[" (-> feeds :entries count) "] feeds"))
    (subdb/update-rss-link id updated)
    ;; if url is updated, feeds should be nil
    (when feeds (db/save-feeds feeds id))))

(defn- mk-fetcher-task [{:keys [url last_modified etag] :as link}]
  (reify IHttpTask
    (getUri [this] (java.net.URI. url))
    (getProxy [this] (:proxy @rssminer-conf))
    (onThrowable [this ^Throwable t]
      (warn (str "id:" (:id link)) url (.getMessage t))
      (try
        (subdb/update-rss-link (:id link)
                               (let [interval (slower (:check_interval link))]
                                 {:check_interval interval
                                  :next_check_ts (+ (now-seconds) interval)
                                  :error_msg (.getMessage t)}))
        (catch Exception e (error e url)))) ; mysql fail
    (getHeaders [this]
      {HttpUtils/IF_MODIFIED_SINCE last_modified
       HttpUtils/IF_NONE_MATCH etag})
    (doTask [this status headers body]
      (try (handle-resp link status headers body)
           (catch Exception e (error e (str "id:" (:id link) url)))))
    (toString [this]
      (str (.getUri this) " " (.getHeaders this)))))

(defn mk-provider []
  (reify IHttpTasksProvder
    (getTasks [this]
      (map mk-fetcher-task (subdb/fetch-rss-links
                            (:fetch-size @rssminer-conf))))))

(defn refetch-rss-link [id]
  (if-let [rss (subdb/fetch-rss-link-by-id id)]
    (fetcher-enqueue rss)))

(defn mk-blocking-provider []
  (reify IBlockingTaskProvider
    (getTask [this timeout]
      (when-let [d (fetcher-dequeue timeout)] ; seconds
        (mk-fetcher-task d)))))

(defn start-fetcher []
  (stop-fetcher)
  (reset! fetcher (doto (HttpTaskRunner.
                         (doto (HttpTaskRunnerConf.)
                           ;; poll database

                           (.setBlockingTimeOut 30) ; 30 second
                           (.setBulkProvider (mk-provider))
                           (.setBlockingProvider (mk-blocking-provider))
                           (.setQueueSize (:fetcher-concurrency
                                           @rssminer-conf))
                           (.setProxy (:proxy @rssminer-conf))
                           (.setName "Fetcher")))
                    (.start))))
