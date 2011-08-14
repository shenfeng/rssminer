(ns rssminer.db.dashboard
  (:use [rssminer.db.util :only [h2-query]]
        [rssminer.time :only [now-seconds]]))

(defn get-crawled-count []
  (-> (h2-query ["SELECT COUNT (*) as count FROM crawler_links WHERE
       next_check_ts > ?" (now-seconds)]) first :count))

(defn get-total-count []
  (-> (h2-query ["SELECT COUNT(*) as count FROM crawler_links"])
      first :count))

(defn get-crawled-links
  [& {:keys [limit offset] :or {limit 40 offset 0}}]
  (h2-query
   ["SELECT url, title, check_interval, next_check_ts AS check_ts, added_ts,
     (SELECT url FROM crawler_links c
             WHERE c.id = cl.referer_id ) AS referer
     FROM crawler_links cl
     WHERE next_check_ts > ?
     ORDER BY next_check_ts DESC LIMIT ? OFFSET ?"
    (now-seconds)  limit offset]))

(defn get-pending-links
  [& {:keys [limit offset] :or {limit 40 offset 0}}]
  (h2-query
   ["SELECT url, title, check_interval, next_check_ts AS check_ts, added_ts,
     (SELECT url FROM crawler_links c
             WHERE c.id = cl.referer_id ) AS referer
     FROM crawler_links cl
     WHERE next_check_ts < ?
     ORDER BY next_check_ts DESC LIMIT ? OFFSET ?"
    (now-seconds) limit offset]))

(defn get-rss-links
  [& {:keys [limit offset] :or {limit 40 offset 0}}]
  (h2-query
   ["SELECT url, title, added_ts,
    (SELECT url FROM crawler_links c
         WHERE c.id = crawler_link_id ) AS referer
     FROM rss_links ORDER BY added_ts DESC LIMIT ? OFFSET ?"
    limit offset]))

