(ns rssminer.crawler
  (:use (rssminer [time :only [now-seconds]]
                  [http :only [parse-response client extract-links]]
                  [util :only [assoc-if next-check]])
        [clojure.tools.logging :only [info error trace]])
  (:require [rssminer.db.crawler :as db]
            [rssminer.config :as conf])
  (:import [rssminer.task HttpTaskRunner IHttpTask IHttpTaskProvder]
           org.jboss.netty.handler.codec.http.HttpResponse))

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

(defn- keep? [url title]                ; url can't be nil
  (and url
       (not (re-find #"(?i)\bcomments" url))
       (not (re-find #"(?i)\bcomments" (or title "")))))

(defn save-links [referer links rsses]
  (doseq [{:keys [url title]} rsses]
    (when (keep? url title)
      (db/insert-rss-link {:url url
                           :title title
                           :next_check_ts (rand-int 100000)
                           :crawler_link_id (:id referer)})))
  (db/insert-crawler-links referer
                           (map #(assoc %
                                   :next_check_ts (rand-int 1000000)
                                   :referer_id (:id referer)) links)))

(defn- mk-task [{:keys [id url check_interval last_modified] :as link}]
  (reify IHttpTask
    (getUri [this] (java.net.URI. url))
    (getProxy [this] conf/no-proxy)
    (getHeaders [this]
      (if last_modified
        {"If-Modified-Since" last_modified} {}))
    (doTask [this resp]
      (let [{:keys [status headers body] :as resp} (parse-response resp)
            {:keys [title links rss]} (when body (extract-links url body))
            updated (assoc-if (next-check check_interval status headers)
                              :last_modified (:last-modified headers)
                              :title title)]
        (db/update-crawler-link id updated)
        (when body
          (save-links link links rss))))))

(defn mk-provider []
  (reify IHttpTaskProvder
    (getTasks [this]
      (map mk-task (db/fetch-crawler-links conf/fetch-size)))))

(defn start-crawler [& {:keys [queue]}]
  (stop-crawler)
  (let [queue (or queue conf/crawler-queue)]
    (reset! crawler (doto (HttpTaskRunner. (mk-provider) client
                                           queue "Crawler" conf/http-proxy)
                      (.start)))))
