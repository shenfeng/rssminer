(ns rssminer.db.user-feed
  (:use [rssminer.db.util :only [mysql-query with-mysql mysql-insert]]
        [rssminer.time :only [now-seconds]]
        [clojure.java.jdbc :only [insert-record update-values]]))

(defn insert-user-vote [user-id feed-id vote]
  (let [old  (-> (mysql-query
                  ["SELECT vote FROM user_feed
                    WHERE user_id = ? AND feed_id =?" user-id feed-id])
                 first :vote)]
    (if (nil? old)
      (mysql-insert :user_feed {:user_id user-id
                             :feed_id feed-id
                             :vote vote})
      (when (not= old vote)
        (with-mysql
          (update-values :user_feed
                         ["user_id = ? AND feed_id = ?" user-id feed-id]
                         {:vote vote}))))))

(defn insert-sys-vote [user-id feed-id vote]
  (let [old  (-> (mysql-query
                  ["SELECT vote_sys FROM user_feed
                    WHERE user_id = ? AND feed_id =?" user-id feed-id])
                 first :vote_sys)]
    (if (nil? old)
      (mysql-insert :user_feed {:user_id user-id
                             :feed_id feed-id
                             :vote_sys vote})
      (when (not= old vote)
        (with-mysql
          (update-values :user_feed
                         ["user_id = ? AND feed_id = ?" user-id feed-id]
                         {:vote_sys vote}))))))

(defn mark-as-read [user-id feed-id]
  (let [old  (-> (mysql-query
                  ["SELECT read_date FROM user_feed
                    WHERE user_id = ? AND feed_id =?" user-id feed-id])
                 first :read_date)]
    (if (nil? old)
      (mysql-insert :user_feed {:user_id user-id
                             :feed_id feed-id
                             :read_date (now-seconds)})
      (when (= old -1)
        (with-mysql
          (update-values :user_feed
                         ["user_id = ? AND feed_id = ?" user-id feed-id]
                         {:read_date (now-seconds)}))))))

(defn fetch-up-ids [user-id]
  (map :feed_id (mysql-query ["SELECT feed_id FROM user_feed
                            WHERE user_id = ? AND vote = 1" user-id])))

(defn fetch-down-ids [user-id]
  (map :feed_id (mysql-query ["SELECT feed_id FROM user_feed
                            WHERE user_id = ? AND vote = -1" user-id])))

(defn fetch-unvoted-feedids [user-id since-time]
  (map :id (mysql-query ["call get_unvoted_feedids(?, ?)" user-id since-time])))

(defn fetch-system-voteup [user-id limit]
  (mysql-query ["SELECT f.id, f.title, f.author, f.tags, f.link, f.rss_link_id,
              f.published_ts, uf.vote, uf.vote_sys
              FROM feeds f JOIN user_feed uf ON uf.feed_id = f.id
              WHERE uf.user_id = ? and uf.vote = 0
              ORDER BY uf.vote_sys desc limit ?" user-id limit]))

(defn fetch-recent-read [user-id limit]
  (mysql-query ["SELECT f.id, f.title, f.author, f.link, f.tags, f.rss_link_id,
              f.published_ts, uf.vote, uf.read_date, uf.vote_sys
              FROM feeds f JOIN user_feed uf ON uf.feed_id = f.id
              WHERE uf.user_id = ? and uf.read_date > 0
              ORDER BY uf.read_date desc limit ?" user-id limit]))

(defn fetch-recent-voted [user-id limit]
  (mysql-query ["SELECT f.id, f.title, f.author, f.link, f.tags, f.rss_link_id,
              f.published_ts, uf.vote, uf.read_date, uf.vote_sys
              FROM feeds f JOIN user_feed uf ON uf.feed_id = f.id
              WHERE uf.user_id = ? and uf.vote != 0
              ORDER BY uf.read_date desc limit ?" user-id limit]))
