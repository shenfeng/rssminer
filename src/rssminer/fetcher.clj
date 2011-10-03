(ns rssminer.fetcher
  (:use [clojure.tools.logging :only [trace]]
        [rssminer.db.crawler :only [update-rss-link fetch-rss-links]]
        (rssminer [util :only [assoc-if next-check]]
                  [parser :only [parse-feed]]
                  [http :only [client parse-response]]))
  (:require [rssminer.db.feed :as db]
            [rssminer.config :as conf])
  (:import [rssminer.task HttpTaskRunner IHttpTask IHttpTaskProvder]))

(defonce fetcher (atom nil))

(defn running? []
  (if-not (nil? @fetcher)
    (.isRunning ^HttpTaskRunner @fetcher)
    false))

(defn stop-fetcher []
  (when (running?)
    (.stop ^HttpTaskRunner @fetcher)))

(defn handle-resp [{:keys [id url check_interval last_modified]}
                   {:keys [status headers body]}]
  (let [feeds (when body (parse-feed body))
        updated (assoc-if (next-check check_interval status headers)
                          :last_modified (:last-modified headers)
                          :alternate (:link feeds)
                          :description (:description feeds)
                          :title (:title feeds))]
    (trace status url (str "(" (-> feeds :entries count) " feeds)"))
    (update-rss-link id updated)
    (when feeds (db/save-feeds feeds id nil))))

(defn- mk-task [{:keys [url last_modified] :as link}]
  (reify IHttpTask
    (getUri [this] (java.net.URI. url))
    (getProxy [this] (if (conf/reseted-url? url)
                       conf/http-proxy conf/no-proxy))
    (getHeaders [this]
      (if last_modified {"If-Modified-Since" last_modified} {}))
    (doTask [this resp]
      (handle-resp link (parse-response resp)))))

(defn mk-provider []
  (reify IHttpTaskProvder
    (getTasks [this]
      (map mk-task (fetch-rss-links conf/fetch-size)))))

(defn start-fetcher [& {:keys [queue]}]
  (stop-fetcher)
  (let [queue (or queue conf/fetcher-queue)]
    (reset! fetcher (doto (HttpTaskRunner. (mk-provider) client
                                           queue "Fetcher" conf/http-proxy)
                      (.start)))))
