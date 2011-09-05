(ns rssminer.crawler
  (:use [rssminer.db.util :only [parse-timestamp]]
        (rssminer [time :only [now-seconds]]
                  [util :only [assoc-if start-tasks]])
        [clojure.tools.logging :only [info error trace]])
  (:require [rssminer.db.crawler :as db]
            [rssminer.http :as http]
            [rssminer.config :as conf])
  (:import [java.util Queue LinkedList]))

(def ^Queue queue (LinkedList.))

(defonce crawler (atom nil))

(defn stop-crawler []
  (when-not (nil? @crawler)
    (info "shutdowning link crawler....")
    (@crawler :shutdown)
    (info "crawler is shutdowned")
    (reset! crawler nil)))

(defn- extract-and-save-links [referer html]
  (let [{:keys [rss links]} (http/extract-links (:url referer) html)]
    (doseq [{:keys [url title]} rss]
      (when-not (and url (re-find #"(?i)\bcomments" (or title "")))
        (db/insert-rss-link {:url url
                             :title title
                             :next_check_ts (conf/rand-ts)
                             :crawler_link_id (:id referer)})))
    (db/insert-crawler-links referer
                             (map #(assoc %
                                     :next_check_ts (conf/rand-ts)
                                     :referer_id (:id referer)) links))))

(defn- extract-title [html]
  (when html (-> (re-seq #"(?im)<title>(.+)</title>" html)
                 first
                 second)))

(defn crawl-link
  [{:keys [id url check_interval] :as referer}]
  (let [{:keys [status headers body] :as resp} (http/get url)
        html (when body (try (slurp body)
                             (catch Exception e
                               (error url e))))
        updated (assoc-if (conf/next-check check_interval html)
                          :last_modified (:last-modified headers)
                          :title (extract-title html))]
    (trace status url)
    (db/update-crawler-link id updated)
    (when html
      (extract-and-save-links referer html))))

(defn get-next-link []
  (locking queue
    (if (.peek queue) ;; has element?
      (.poll queue)   ;; retrieves and removes
      (let [links (db/fetch-crawler-links conf/fetch-size)]
        (trace "fetch" (count links) "crawler links from h2")
        (when (seq links)
          (doseq [link links]
            (.offer queue link))
          (get-next-link))))))

(defn start-crawler [& {:keys [threads]}]
  (stop-crawler)
  (reset! crawler (start-tasks get-next-link crawl-link "crawler"
                               (or threads conf/crawler-threads-count)))
  (info "link crawler started")
  @crawler)
