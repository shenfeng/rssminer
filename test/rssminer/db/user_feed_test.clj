(ns rssminer.db.user-feed-test
  (:use clojure.test
        rssminer.db.user-feed
        [rssminer.db.util :only [mysql-query with-mysql mysql-insert-and-return]]
        [clojure.java.jdbc :only [delete-rows]]
        (rssminer [test-common :only [user1 app-fixture mk-feeds-fixtrue]])))

(use-fixtures :each app-fixture (mk-feeds-fixtrue "test/scottgu-atom.xml" ))

(deftest test-insert-user-vote
  (let [fid (-> (mysql-query ["select id from feeds"]) first :id)]
    (insert-user-vote (:id user1) fid 1)
    (is (= 1 (-> (mysql-query
                  ["SELECT vote FROM user_feed WHERE
                    user_id = ? AND feed_id = ?" (:id user1) fid])
                 first :vote)))
    (insert-user-vote (:id user1) fid -1)
    (is (= -1 (-> (mysql-query
                   ["SELECT vote FROM user_feed WHERE
                    user_id = ? AND feed_id = ?" (:id user1) fid])
                  first :vote)))))

(deftest test-insert-read-date
  (let [fid (-> (mysql-query ["select id from feeds"]) first :id)]
    (mark-as-read (:id user1) fid)
    (is (< 1 (-> (mysql-query
                  ["SELECT read_date FROM user_feed WHERE
                    user_id = ? AND feed_id = ?" (:id user1) fid])
                 first :read_date)))))

(deftest test-fetch-unvoted-feedids
  (is (= 1 (count (fetch-unvoted-feedids (:id user1) 0))))
  (let [f1 (-> (mysql-query ["select id from feeds"]) first :id)]
    (insert-user-vote (:id user1) f1 0)
    (is (= 1 (count (fetch-unvoted-feedids (:id user1) 0))))
    (insert-user-vote (:id user1) f1 1)
    (is (= 0 (count (fetch-unvoted-feedids (:id user1) 0))))))

