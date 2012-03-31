(ns rssminer.db.crawler
  (:use [clojure.tools.logging :only [info]]
        (rssminer [config :only [multi-domain?]]
                  [time :only [now-seconds]]
                  [util :only [ignore-error]])
        [rssminer.db.util :only [mysql-query with-mysql]]
        [clojure.java.jdbc :only [with-connection with-query-results
                                  insert-record update-values]]))
(defn fetch-crawler-links [limit]
  "Returns nil when no more"
  (mysql-query ["SELECT id, url, check_interval
              FROM crawler_links
              WHERE next_check_ts < ?
              ORDER BY next_check_ts LIMIT ? " (now-seconds) limit]))

(defn- insert-crawler-link [link]
  (ignore-error ;; ignore voilation of uniqe constraint
   (with-mysql
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
  (with-mysql
    (update-values :crawler_links ["id = ?" id] data)))

