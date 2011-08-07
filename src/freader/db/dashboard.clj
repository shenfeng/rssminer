(ns freader.db.dashboard
  (:use [freader.db.util :only [h2-query]]))

(defn get-crawled-count []
  (-> (h2-query
       ["SELECT COUNT (*) as count FROM crawler_links
         WHERE DATEDIFF('SECOND', last_check_ts, NOW()) > check_interval"])
      first :count))

(defn get-total-count []
  (-> (h2-query ["SELECT COUNT(*) as count FROM crawler_links"])
      first :count))

(defn get-crawled-links
  [& {:keys [limit offset] :or {limit 40 offset 0}}]
  (h2-query
   ["SELECT url, title, check_interval, last_check_ts AS check_ts,
     (SELECT url FROM crawler_links c
             WHERE c.id = cl.referer_id ) AS referer
     FROM crawler_links cl
     WHERE DATEDIFF('SECOND', last_check_ts, NOW()) < check_interval
     ORDER BY last_check_ts
     DESC LIMIT ? OFFSET ?"  limit offset]))

(defn get-pending-links
  [& {:keys [limit offset] :or {limit 40 offset 0}}]
  (h2-query
   ["SELECT url, title, check_interval, last_check_ts AS check_ts,
     (SELECT url FROM crawler_links c
             WHERE c.id = cl.referer_id ) AS referer
     FROM crawler_links cl
     WHERE DATEDIFF('SECOND', last_check_ts, NOW()) > check_interval
     ORDER BY last_check_ts DESC LIMIT ? OFFSET ?"
    limit offset]))

(defn get-rss-links
  [& {:keys [limit offset] :or {limit 40 offset 0}}]
  (h2-query
   ["SELECT url, title, added_ts,
    (SELECT url FROM crawler_links c
         WHERE c.id = crawler_link_id ) AS referer
     FROM rss_links LIMIT ? OFFSET ?"
    limit offset]))

