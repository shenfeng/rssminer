(ns rssminer.db.dashboard
  (:use [rssminer.db.util :only [h2-query]]
        [rssminer.time :only [now-seconds]]))

(defn crawled-count []
  (-> (h2-query ["SELECT COUNT (*) as count FROM crawler_links
                  WHERE next_check_ts > ?" (now-seconds)]) first :count))

(defn rss-links-count []
  (-> (h2-query ["SELECT COUNT (*) as count FROM rss_links"])
      first :count))

(defn finished-count []
  (-> (h2-query ["SELECT COUNT (*) as count FROM rss_links
                  WHERE next_check_ts > ?" (now-seconds)])
      first :count))

(defn crawler-links-count []
  (-> (h2-query ["SELECT COUNT(*) as count FROM crawler_links"])
      first :count))

(defn feeds-count []
  (-> (h2-query ["SELECT COUNT(*) as count FROM feeds"])
      first :count))

(defn get-crawled-links
  [& {:keys [limit offset] :or {limit 30 offset 0}}]
  (h2-query
   ["SELECT id, url, title, check_interval, next_check_ts, added_ts,
     (SELECT url FROM crawler_links c
             WHERE c.id = cl.referer_id ) AS referer
     FROM crawler_links cl
     WHERE next_check_ts > ?
     ORDER BY next_check_ts DESC LIMIT ? OFFSET ?"
    (now-seconds)  limit offset]))

(defn get-pending-links
  [& {:keys [limit offset] :or {limit 30 offset 0}}]
  (h2-query
   ["SELECT id, url, title, check_interval, next_check_ts, added_ts,
     (SELECT url FROM crawler_links c
             WHERE c.id = cl.referer_id ) AS referer
     FROM crawler_links cl
     WHERE next_check_ts < ?
     ORDER BY next_check_ts DESC LIMIT ? OFFSET ?"
    (now-seconds) limit offset]))

(defn get-rss-links
  [& {:keys [limit offset] :or {limit 30 offset 0}}]
  (h2-query
   ["SELECT id, url, title, check_interval, next_check_ts, added_ts
    FROM rss_links WHERE id > (SELECT MAX(id) FROM rss_links) - ?
    ORDER BY id DESC LIMIT ? OFFSET ?"
    (+ limit offset) limit offset]))

