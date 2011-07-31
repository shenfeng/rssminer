(ns freader.db.crawler
  (:use [freader.database :only [h2-db-factory]]
        [freader.http :only [extract-host]]
        [freader.db.util :only [h2-query]]
        [clojure.java.jdbc :only [with-connection with-query-results
                                  insert-record update-values]])
  (:import java.util.Date))

(defn fetch-crawler-links
  "Returns nil when no more"
  ([] (fetch-crawler-links 5))
  ([limit]
     (h2-query
      ["SELECT id, url, last_status, last_md5, check_interval,
        DATEDIFF('SECOND', last_check_ts, NOW()) AS interval
        FROM crawler_link
        WHERE DATEDIFF('SECOND', last_check_ts, NOW()) > check_interval
        ORDER BY interval LIMIT ? " limit])))

(defn insert-crawler-links
  "Save links to crawler_link, return generated ids of inserted ones"
  [referer links]
  (let [multi-domains (set
                       (map :domain
                            (h2-query ["select * from multi_rss_domain"])))
        f (fn [{:keys [href title]}]
            (let [domain (extract-host href)]
              (when (or (multi-domains domain)
                        (nil? (h2-query ["SELECT domain FROM crawler_link
                                          WHERE domain = ?" domain])))
                (try
                  (with-connection @h2-db-factory
                    (insert-record :crawler_link {:url href
                                                  :title title
                                                  :domain domain
                                                  :referer_id (:id referer)}))
                  ;; ignore voilation of uniqe constraint
                  (catch Exception e)))))]
    (filter identity (doall (map f links)))))

(defn update-crawler-link [link]
  (with-connection @h2-db-factory
    (update-values :crawler_link ["id = ?" (:id link)]
                   (assoc (dissoc link :id)
                     :last_check_ts (Date.)))))

(defn fetch-rss-links
  ([] (fetch-rss-links 10))
  ([limit]
     (h2-query
      ["SELECT id, url, check_interval,
       DATEDIFF('SECOND', last_check_ts, NOW()) AS interval
       FROM rss_link
       WHERE DATEDIFF('SECOND', last_check_ts, NOW()) > check_interval
       ORDER BY interval LIMIT ? " limit])))

(defn insert-rss-link
  "Silently ignore duplicate link"
  [link]
  (with-connection @h2-db-factory
    (try (insert-record :rss_link link)
         ;; ignore voilate of uniqe constraint
         (catch Exception e))))

(defn update-rss-link [link]
  "Will take care of update last check timestamp"
  (with-connection @h2-db-factory
    (update-values :rss_link ["id = ?" (:id link)]
                   (assoc (dissoc link :id)
                     :last_check_ts (Date.)))))

