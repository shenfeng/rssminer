(ns rssminer.db.crawler
  (:use [rssminer.database :only [h2-db-factory]]
        [rssminer.time :only [now-seconds]]
        [clojure.tools.logging :only [info]]
        [rssminer.util :only [ignore-error]]
        [rssminer.db.util :only [h2-query]]
        [clojure.java.jdbc :only [with-connection with-query-results
                                  insert-record update-values]]))
(defn fetch-crawler-links
  "Returns nil when no more"
  ([] (fetch-crawler-links 5))
  ([limit]
     (h2-query
      ["SELECT id, url, last_md5, check_interval
        FROM crawler_links
        WHERE next_check_ts < ?
        ORDER BY next_check_ts LIMIT ? "
       (now-seconds) limit])))

(defn get-multi-domains []
  (set (map :domain
            (h2-query ["SELECT * FROM multi_rss_domains"]))))

(defn insert-crawler-link [link]
  (ignore-error ;; ignore voilation of uniqe constraint
   (with-connection @h2-db-factory
     (insert-record :crawler_links link))))

(defn insert-crawler-links
  "Save links to crawler_link, return generated ids of inserted ones"
  [refer links]
  (let [multi-domains (get-multi-domains)
        grouped (group-by :domain links)
        f (fn [[domain links]]
            (cond
             (multi-domains domain) (map insert-crawler-link links)
             (nil? (h2-query ["SELECT domain FROM crawler_links
                                          WHERE domain = ?" domain]))
             (insert-crawler-link (first links))))]
    ;; (when (> (count grouped) 20)
    ;;   (info "domains" (count grouped) (:url refer) (keys grouped)))
    (doall (filter identity (flatten (map f grouped))))))

(defn update-crawler-link [id data]
  (with-connection @h2-db-factory
    (update-values :crawler_links ["id = ?" id] data)))

(defn fetch-rss-links
  ([] (fetch-rss-links 10))
  ([limit]
     (h2-query
      ["SELECT id, url, last_md5, check_interval
       FROM rss_links
       WHERE next_check_ts < ?
       ORDER BY next_check_ts DESC LIMIT ?" (now-seconds) limit])))

(defn insert-rss-link
  "Silently ignore duplicate link"
  [link]
  (ignore-error ;; ignore voilate of uniqe constraint
   (with-connection @h2-db-factory
     (insert-record :rss_links link))))

(defn update-rss-link [id data]
  (with-connection @h2-db-factory
    (update-values :rss_links ["id = ?" id] data)))

(defn fetch-rss-links
  "Returns nil when no more"
  ([] (fetch-rss-links 5))
  ([limit] (h2-query
            ["SELECT id, url, last_md5, check_interval, last_modified
              FROM rss_links
              WHERE next_check_ts < ?
              ORDER BY next_check_ts LIMIT ?" (now-seconds) limit])))
