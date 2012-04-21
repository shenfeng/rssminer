(ns rssminer.fetcher
  (:use [clojure.tools.logging :only [trace]]
        (rssminer [util :only [assoc-if]]
                  [parser :only [parse-feed]]
                  [time :only [now-seconds]]
                  [redis :only [fetcher-dequeue]]
                  [http :only [parse-response]]
                  [config :only [rssminer-conf]]))
  (:require [rssminer.db.feed :as db])
  (:import [rssminer.task HttpTaskRunner IHttpTask IHttpTasksProvder
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
  (min (int (* last-interval 1.4)) (* 3600 24 2))) ;2 days

(defn- quicker [last-interval]
  (max 7200 (int (/ last-interval 1.4)))) ; min 2h

(defn- next-check [last-interval status headers]
  (if-let [location (get headers HttpUtils/LOCATION)]
    {:url location :next_check_ts (rand-int 100000)}
    (let [interval (if (= 200 status)
                     (quicker last-interval) (slower last-interval))]
      {:check_interval interval
       :next_check_ts (+ (now-seconds) interval)})))

(defn handle-resp [{:keys [id url check_interval last_modified]}
                   status headers body]
  (let [feeds (when (and (= 200 status) body) (parse-feed body))
        updated (assoc-if (next-check check_interval status headers)
                          :last_modified (get headers HttpUtils/LAST_MODIFIED)
                          :alternate (:link feeds)
                          :last_status status
                          :description (:description feeds)
                          :title (:title feeds))]
    (trace (str "id:" id) status url
           (str "[" (-> feeds :entries count) "] feeds"))
    (db/update-rss-link id updated)
    (when feeds (db/save-feeds feeds id))))

(defn- mk-task [{:keys [url last_modified] :as link}]
  (reify IHttpTask
    (getUri [this] (java.net.URI. url))
    (getProxy [this] (:proxy @rssminer-conf))
    (onThrowable [this t]
      (db/update-rss-link (:id link)
                          (let [interval (slower (:check_interval link))]
                            {:check_interval interval
                             :next_check_ts (+ (now-seconds) interval)
                             :error_msg (.getMessage ^Throwable t)})))
    (getHeaders [this]
      (if last_modified {"If-Modified-Since" last_modified} {}))
    (doTask [this status headers body]
      (handle-resp link status headers body))))

(defn mk-provider []
  (reify IHttpTasksProvder
    (getTasks [this]
      (map mk-task (db/fetch-rss-links (:fetch-size @rssminer-conf))))))

(defn mk-blocking-provider []
  (reify IBlockingTaskProvider
    (getTask [this timeout]
      (when-let [d (fetcher-dequeue timeout)]
        (mk-task d)))))

(defn start-fetcher []
  (stop-fetcher)
  (reset! fetcher (doto (HttpTaskRunner.
                         (doto (HttpTaskRunnerConf.)
                           ;; poll database
                           (.setBulkCheckInterval (* 15  60  1000)) ; 15 min
                           (.setBlockingTimeOut 20) ; 15 second
                           (.setBulkProvider (mk-provider))
                           (.setBlockingProvider (mk-blocking-provider))
                           (.setQueueSize (:fetcher-queue @rssminer-conf))
                           (.setProxy (:proxy @rssminer-conf))
                           (.setName "Fetcher")))
                    (.start))))
