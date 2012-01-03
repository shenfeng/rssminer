(ns rssminer.db.user-feed
  (:use [rssminer.db.util :only [h2-query with-h2 h2-insert]]
        [rssminer.time :only [now-seconds]]
        [clojure.java.jdbc :only [insert-record update-values]]))

(defn insert-user-vote [user-id feed-id vote]
  (let [old  (-> (h2-query
                  ["SELECT vote FROM user_feed
                    WHERE user_id = ? AND feed_id =?" user-id feed-id])
                 first :vote)]
    (if (nil? old)
      (h2-insert :user_feed {:user_id user-id
                             :feed_id feed-id
                             :vote vote})
      (when (not= old vote)
        (with-h2
          (update-values :user_feed
                         ["user_id = ? AND feed_id = ?" user-id feed-id]
                         {:vote vote}))))))

(defn insert-sys-vote [user-id feed-id vote]
  (let [old  (-> (h2-query
                  ["SELECT vote FROM user_feed
                    WHERE user_id = ? AND feed_id =?" user-id feed-id])
                 first :vote_sys)]
    (if (nil? old)
      (h2-insert :user_feed {:user_id user-id
                             :feed_id feed-id
                             :vote_sys vote})
      (when (not= old vote)
        (with-h2
          (update-values :user_feed
                         ["user_id = ? AND feed_id = ?" user-id feed-id]
                         {:vote_sys vote}))))))

(defn mark-as-read [user-id feed-id]
  (let [old  (-> (h2-query
                  ["SELECT read_date FROM user_feed
                    WHERE user_id = ? AND feed_id =?" user-id feed-id])
                 first :read_date)]
    (if (nil? old)
      (h2-insert :user_feed {:user_id user-id
                             :feed_id feed-id
                             :read_date (now-seconds)})
      (when (= old -1)
        (with-h2
          (update-values :user_feed
                         ["user_id = ? AND feed_id = ?" user-id feed-id]
                         {:read_date (now-seconds)}))))))

(defn fetch-up-ids [user-id]
  (map :feed_id (h2-query ["SELECT feed_id FROM user_feed
                            WHERE user_id = ? AND vote = 1" user-id])))

(defn fetch-down-ids [user-id]
  (map :feed_id (h2-query ["SELECT feed_id FROM user_feed
                            WHERE user_id = ? AND vote = -1" user-id])))

(defn fetch-unvoted-feedids [user-id since-time]
  (map :id (h2-query ["SELECT f.id FROM feeds f
              JOIN user_subscription us on us.rss_link_id = f.rss_link_id
              WHERE us.user_id = ? and f.published_ts > ?
              EXCEPT
              SELECT feed_id AS id FROM user_feed
              WHERE user_id = ? AND vote != 0"
                      user-id since-time user-id])))
