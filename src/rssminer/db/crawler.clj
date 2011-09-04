(ns rssminer.db.crawler
  (:use [clojure.tools.logging :only [info]]
        (rssminer [config :only [multi-domain?]]
                  [time :only [now-seconds]]
                  [util :only [ignore-error]])
        [rssminer.db.util :only [h2-query with-h2]]
        [clojure.java.jdbc :only [with-connection with-query-results
                                  insert-record update-values]]))
(defn fetch-crawler-links [limit]
  "Returns nil when no more"
  (h2-query ["SELECT id, url, check_interval
              FROM crawler_links
              WHERE next_check_ts < ?
              ORDER BY next_check_ts LIMIT ? " (now-seconds) limit]))

(defn- insert-crawler-link [link]
  (ignore-error ;; ignore voilation of uniqe constraint
   (with-h2
     (insert-record :crawler_links link))))

(defn insert-crawler-links
  "Save links to crawler_link, return generated ids of inserted ones"
  [refer links]
  (let [grouped (group-by :domain links)
        f (fn [[domain group]]
            (if (multi-domain? domain)
              (map #(insert-crawler-link
                     (assoc % :domain nil)) group)
              (insert-crawler-link (first group))))]
    (doall (filter identity (flatten (map f grouped))))))

(defn update-crawler-link [id data]
  (with-h2
    (update-values :crawler_links ["id = ?" id] data)))

(defn insert-rss-link
  [link]
  (ignore-error ;; ignore voilate of uniqe constraint
   (with-h2
     (insert-record :rss_links link))))

(defn update-rss-link [id data]
  (with-h2
    (update-values :rss_links ["id = ?" id] data)))

(defn fetch-rss-links [limit]
  "Returns nil when no more"
  (h2-query ["SELECT id, url, check_interval, last_modified
              FROM rss_links
              WHERE next_check_ts < ?
              ORDER BY next_check_ts LIMIT ?" (now-seconds) limit]))
