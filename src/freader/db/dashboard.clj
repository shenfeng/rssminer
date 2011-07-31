(ns freader.db.dashboard
  (:use [freader.db.util :only [h2-query]]))

(defn get-crawled-count []
  (-> (h2-query
       ["SELECT COUNT (*) as count FROM crawler_link
         WHERE DATEDIFF('SECOND', last_check_ts, NOW()) > check_interval"])
      first :count))

(defn get-total-count []
  (-> (h2-query ["SELECT COUNT(*) as count FROM crawler_link"])
      first :count))

(defn get-crawled-links
  [& {:keys [limit offset] :or {limit 40 offset 0}}]
  (h2-query
   ["SELECT url, title, check_interval,
     last_check_ts as check_ts, last_status,
     (select url from crawler_link i where i.id = cl.referer_id ) as referer
     FROM crawler_link cl
     WHERE DATEDIFF('SECOND', last_check_ts, NOW()) < check_interval
     ORDER BY last_check_ts DESC LIMIT ? OFFSET ?"
    limit offset]))

(defn get-pending-links
  [& {:keys [limit offset] :or {limit 40 offset 0}}]
  (h2-query
   ["SELECT url, title, check_interval,
     last_check_ts as check_ts, last_status,
     (select url from crawler_link i where i.id = cl.referer_id ) as referer
     FROM crawler_link cl
     WHERE DATEDIFF('SECOND', last_check_ts, NOW()) > check_interval
     ORDER BY last_check_ts DESC LIMIT ? OFFSET ?"
    limit offset]))
