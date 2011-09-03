(ns rssminer.fetcher
  (:use [rssminer.util :only [assoc-if start-tasks]]
        [clojure.tools.logging :only [error info trace]]
        [rssminer.db.crawler :only [update-rss-link fetch-rss-links]]
        [rssminer.parser :only [parse-feed]])
  (:require [rssminer.db.feed :as db]
            [rssminer.http :as http]
            [rssminer.config :as conf])
  (:import [java.util Queue LinkedList]))

(def ^Queue queue (LinkedList.))

(defonce fetcher (atom nil))

(defn stop-fetcher []
  (when-not (nil? @fetcher)
    (info "shutdowning rss fetcher....")
    (@fetcher :shutdown)
    (info "fetch is shutdowned")
    (reset! fetcher nil)))

(defn fetch-rss
  [{:keys [id url check_interval last_modified] :as link}]
  (let [{:keys [status headers body]} (http/get url
                                                :last-modified last_modified)
        html (when body (try (slurp body)
                             (catch Exception e
                               (error e url))))
        feeds (when html (parse-feed html))
        updated (assoc-if (conf/next-check check_interval html)
                          :last_modified (:last-modified headers)
                          :alternate (:link feeds)
                          :description (:description feeds)
                          :title (:title feeds))]
    (trace status url (str "(" (-> feeds :entries count) " feeds)"))
    (update-rss-link id updated)
    (when feeds (db/save-feeds feeds id nil))))

(defn get-next-link []
  (locking queue
    (if (.peek queue) ;; has element?
      (.poll queue)   ;; retrieves and removes
      (let [links (fetch-rss-links conf/fetch-size)]
        (trace "fetch" (count links) "rss links from h2")
        (when (seq links)
          (doseq [link links]
            (.offer queue link))
          (get-next-link))))))

(defn start-fetcher [& {:keys [threads]}]
  (stop-fetcher)
  (reset! fetcher (start-tasks get-next-link fetch-rss "fetcher"
                               (or threads conf/fetcher-threads-count)))
  (info "rss fetcher started")
  @fetcher)
