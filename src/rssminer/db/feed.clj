(ns rssminer.db.feed
  (:use [rssminer.database :only [mysql-query with-mysql mysql-insert]]
        (rssminer [search :only [index-feed]]
                  [redis :only [redis-pool]]
                  [util :only [to-int now-seconds ignore-error]]
                  [config :only [rssminer-conf cfg]]
                  [classify :only [on-fetcher-event]])
        [clojure.string :only [blank?]]
        [clojure.tools.logging :only [warn]]
        [clojure.java.jdbc :only [do-prepared]])
  (:import rssminer.db.MinerDAO
           rssminer.Utils)
  (:require [clojure.string :as str]))

(defn update-total-feeds [rssid]
  (with-mysql (do-prepared "UPDATE rss_links SET total_feeds =
                 (SELECT COUNT(*) FROM feeds where rss_link_id = ?)
                 WHERE id = ?" [rssid rssid])))

(defn- feed-exits? [feed rssid]
  (if (= -1 (:simhash feed))
    (mysql-query
     ["SELECT 1 FROM feeds WHERE rss_link_id = ? AND link = ?"
      rssid (:link feed)])
    (mysql-query
     ["SELECT 1 FROM feeds WHERE rss_link_id = ? AND simhash = ?"
      rssid (:simhash feed)])))

(defn- save-feed [feed rssid]
  (try (let [id (mysql-insert :feeds (dissoc (assoc feed :rss_link_id rssid)
                                             :summary))]
         (index-feed id rssid feed)
         (mysql-insert :feed_data {:id id :summary (:summary feed)})
         id)                            ; return id
       (catch Exception e
         (warn "insert for rss" rssid e))))

(defn save-feeds [feeds rssid]
  (let [ids (map (fn [feed] (if-not (feed-exits? feed rssid)
                             (save-feed feed rssid)))
                 (:entries feeds))
        inserted (filter identity (doall ids))]
    (when (seq inserted)
      (on-fetcher-event rssid (map to-int inserted))
      (update-total-feeds rssid))))

(defn fetch-link [id]
  (:link (first (mysql-query ["SELECT link FROM feeds WHERE id = ?" id]))))

(defn fetch-feeds [userid ids]
  (let [^MinerDAO db (MinerDAO. (cfg :data-source)
                                (cfg :redis-server))]
    (.fetchFeedsWithSummary db userid ids)))

(defn- get-rssid-by-feedid [id]
  (-> (mysql-query ["select rss_link_id from feeds where id = ?" id])
      first :rss_link_id))

;;; TODO. when autoCommit=false this complete,
;;; other threads does not see the change
(defn insert-user-vote [user-id feed-id vote]
  (let [now (now-seconds)
        rssid (get-rssid-by-feedid feed-id)]
    (when rssid
      (with-mysql (do-prepared ;; rss_link_id default 0, which is ok
                   "INSERT INTO user_feed
                  (user_id, feed_id, rss_link_id, vote_user, vote_date) VALUES(?, ?, ?, ?, ?)
                 ON DUPLICATE KEY UPDATE vote_user = ?, vote_date = ?"
                   [user-id feed-id rssid vote now vote now])))))

(defn mark-as-read [user-id feed-id]
  (let [now (now-seconds)
        rssid (get-rssid-by-feedid feed-id)]
    (when rssid
      (with-mysql (do-prepared ;; rss_link_id default 0
                   "INSERT INTO user_feed (user_id, feed_id, rss_link_id, read_date)
       VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE read_date = ?"
                   [user-id feed-id rssid now now]))
      (Utils/zrem @redis-pool user-id rssid feed-id))))

(def update-sql ["update user_feed set read_time = read_time + ? where user_id = ? and feed_id = ?"])

;; data is a map {:feed_id time}
(defn update-reading-time [user-id data]
  (ignore-error                         ; read_time may out of range, MEDIUMINT UNSIGNED
   (with-mysql
     (apply do-prepared
            (concat update-sql
                    (map (fn [[feedid time]]
                           ;; max 5 minutes, time in 0.1s
                           [(min (* 10 60 5) time)
                            user-id (to-int (name feedid))]) data))))))

(defn- dedup [feeds]
  {:count (count feeds)
   :feeds (MinerDAO/removeDuplicate feeds)})

(defmacro defg [func]
  (let [method (symbol
                (str/replace (name func) #"-(\w)"
                             (fn [[m l]] (str "G"(str/upper-case l)))))]
    `(defn ~func [userid# limit# offset#]
       (let [^MinerDAO db# (MinerDAO. (cfg :data-source)
                                      (cfg :redis-server))]
         (dedup (. db# ~method userid# limit# offset#))))))

(doseq [m '[fetch-newest fetch-likest fetch-read fetch-vote]]
  (eval `(defg ~m)))

(defmacro defsub [func]
  (let [method (symbol
                (str/replace (name func) #"-(\w)"
                             (fn [[m l]] (str/upper-case l))))]
    `(defn ~func [userid# subid# limit# offset#]
       (let [^MinerDAO db# (MinerDAO. (cfg :data-source)
                                      (cfg :redis-server))]
         (dedup (. db# ~method userid# subid# limit# offset#))))))

(doseq [m '[fetch-sub-newest fetch-sub-oldest fetch-sub-likest
            fetch-sub-read fetch-sub-vote]]
  (eval `(defsub ~m)))

(doseq [m '[fetch-folder-newest fetch-folder-oldest fetch-folder-likest
            fetch-folder-read fetch-folder-vote]]
  (eval `(defsub ~m)))
