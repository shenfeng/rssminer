(ns rssminer.crawler
  (:use (rssminer [time :only [now-seconds]]
                  [http :only [parse-response client extract-links links]]
                  [util :only [assoc-if next-check]]
                  [config :only [rssminer-conf]])
        [rssminer.db.feed :only [insert-rss-link]]
        [clojure.tools.logging :only [info error trace]])
  (:require [rssminer.db.crawler :as db])
  (:import [rssminer.task HttpTaskRunner IHttpTask IHttpTasksProvder
            HttpTaskRunnerConf]))

(defonce crawler (atom nil))

(defn running? []
  (if-not (nil? @crawler)
    (.isRunning ^HttpTaskRunner @crawler)
    false))

(defn stop-crawler []
  (when (running?)
    (.stop ^HttpTaskRunner @crawler)))

(defn crawler-stat []
  (when-not (nil? @crawler)
    (.getStat ^HttpTaskRunner @crawler)))

(defn save-links [referer links rsses]
  (doseq [{:keys [url title]} rsses]
    (insert-rss-link {:url url
                      :title title
                      :next_check_ts (rand-int 10000000)}))
  (db/insert-crawler-links referer
                           (map #(assoc %
                                   :next_check_ts (rand-int 10000000)
                                   :referer_id (:id referer)) links)))

(defn handle-resp [{:keys [id url check_interval] :as link}
                   {:keys [status headers body]}]
  (let [{:keys [title links rss]} (when body (extract-links url body))
        updated (assoc-if (next-check check_interval status headers)
                          :last_modified (:last-modified headers)
                          :title title)]
    (db/update-crawler-link id updated)
    (when body
      (save-links link links rss))))

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
      (map mk-task (db/fetch-crawler-links (:fetch-size @rssminer-conf))))))

(defn start-crawler []
  (stop-crawler)
  (reset! crawler (doto (HttpTaskRunner.
                         (doto (HttpTaskRunnerConf.)
                           (.setBulkProvider (mk-provider))
                           (.setClient client)
                           (.setLinks links)
                           (.setQueueSize (:crawler-queue @rssminer-conf))
                           (.setName "Crawler")
                           (.setProxy (:proxy @rssminer-conf))
                           (.setDnsPrefetch (:dns-prefetch @rssminer-conf))))
                    (.start))))
