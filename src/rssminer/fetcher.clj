(ns rssminer.fetcher
  (:use [clojure.tools.logging :only [trace]]
        (rssminer [util :only [assoc-if next-check]]
                  [parser :only [parse-feed]]
                  [redis :only [fetcher-dequeue]]
                  [http :only [client parse-response links]]
                  [config :only [rssminer-conf]]))
  (:require [rssminer.db.feed :as db])
  (:import [rssminer.task HttpTaskRunner IHttpTask IHttpTasksProvder
            HttpTaskRunnerConf IBlockingTaskProvider]))

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

(defn handle-resp [{:keys [id url check_interval last_modified]}
                   {:keys [status headers body]}]
  (let [feeds (when body (parse-feed body))
        updated (assoc-if (next-check check_interval status headers)
                          :last_modified (:last-modified headers)
                          :alternate (:link feeds)
                          :description (:description feeds)
                          :title (:title feeds))]
    (trace status url "(" (-> feeds :entries count) " feeds)")
    (db/update-rss-link id updated)
    (when feeds (db/save-feeds feeds id))))

(defn- mk-task [{:keys [url last_modified] :as link}]
  (reify IHttpTask
    (getUri [this] (java.net.URI. url))
    (getProxy [this] (:proxy @rssminer-conf))
    (getHeaders [this]
      (if last_modified {"If-Modified-Since" last_modified} {}))
    (doTask [this resp]
      (handle-resp link (parse-response resp)))))

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
                           (.setClient client)
                           (.setLinks links)
                           (.setBulkCheckInterval (* 15  60  1000)) ; 15 min
                           (.setBlockingTimeOut 5) ; 5 second
                           (.setBulkProvider (mk-provider))
                           (.setBlockingProvider (mk-blocking-provider))
                           (.setQueueSize (:fetcher-queue @rssminer-conf))
                           (.setProxy (:proxy @rssminer-conf))
                           (.setName "Fetcher")
                           (.setDnsPrefetch (:dns-prefetch @rssminer-conf))))
                    (.start))))
