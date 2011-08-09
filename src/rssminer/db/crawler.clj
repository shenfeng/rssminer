(ns rssminer.db.crawler
  (:use [rssminer.database :only [h2-db-factory]]
        [rssminer.http :only [extract-host]]
        [rssminer.time :only [now-seconds]]
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
        ORDER BY next_check_ts DESC LIMIT ? "
       (now-seconds) limit])))

(defn insert-crawler-links
  "Save links to crawler_link, return generated ids of inserted ones"
  [referer links]
  (let [multi-domains (set
                       (map :domain
                            (h2-query ["select * from multi_rss_domains"])))
        f (fn [{:keys [url title]}]
            (let [domain (extract-host url)]
              (when (or (multi-domains domain)
                        (nil? (h2-query ["SELECT domain FROM crawler_links
                                          WHERE domain = ?" domain])))
                (try
                  (with-connection @h2-db-factory
                    (insert-record :crawler_links {:url url
                                                   :title title
                                                   :domain domain
                                                   :referer_id (:id referer)}))
                  ;; ignore voilation of uniqe constraint
                  (catch Exception e)))))]
    (filter identity (doall (map f links)))))

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
  (with-connection @h2-db-factory
    (try (insert-record :rss_links link)
         ;; ignore voilate of uniqe constraint
         (catch Exception e))))

(defn update-rss-link [id data]
  (with-connection @h2-db-factory
    (update-values :rss_links ["id = ?" id] data)))

