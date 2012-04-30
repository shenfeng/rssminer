(ns rssminer.db.user-feed
  (:use [rssminer.db.util :only [mysql-query with-mysql]]
        [rssminer.time :only [now-seconds]]
        [clojure.java.jdbc :only [do-commands]]))

(defn insert-user-vote [user-id feed-id vote]
  (with-mysql (do-commands ;; rss_link_id default 0, which is ok
               (format "INSERT INTO user_feed (user_id, feed_id, vote_user)
             VALUES (%d, %d, %d) ON DUPLICATE KEY UPDATE vote_user = %d"
                       user-id feed-id vote vote))))

(defn mark-as-read [user-id feed-id]
  (let [now (now-seconds)]
    (with-mysql (do-commands ;; rss_link_id default 0
                 (format "INSERT INTO user_feed (user_id, feed_id, read_date)
       VALUES (%d, %d, %d) ON DUPLICATE KEY UPDATE read_date = %d"
                         user-id feed-id now now)))))

(defn fetch-system-voteup [user-id limit]
  (mysql-query ["SELECT f.id, f.title, f.author, f.tags, f.link, f.rss_link_id,
              f.published_ts, uf.vote_user, uf.vote_sys
              FROM feeds f JOIN user_feed uf ON uf.feed_id = f.id
              WHERE uf.user_id = ? and uf.vote_user = 0
              ORDER BY uf.vote_sys desc limit ?" user-id limit]))

(defn fetch-recent-read [user-id limit]
  (mysql-query ["SELECT f.id, f.title, f.author, f.link, f.tags, f.rss_link_id,
              f.published_ts, uf.vote_user, uf.read_date, uf.vote_sys
              FROM feeds f JOIN user_feed uf ON uf.feed_id = f.id
              WHERE uf.user_id = ? and uf.read_date > 0
              ORDER BY uf.read_date desc limit ?" user-id limit]))

(defn fetch-recent-voted [user-id limit]
  (mysql-query ["SELECT f.id, f.title, f.author, f.link, f.tags, f.rss_link_id,
              f.published_ts, uf.vote_user, uf.read_date, uf.vote_sys
              FROM feeds f JOIN user_feed uf ON uf.feed_id = f.id
              WHERE uf.user_id = ? and uf.vote_user != 0
              ORDER BY uf.read_date desc limit ?" user-id limit]))
