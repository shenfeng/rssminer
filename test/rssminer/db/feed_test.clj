(ns rssminer.db.feed-test
  (:use clojure.test
        rssminer.db.feed
        [rssminer.database :only [mysql-query with-mysql
                                  mysql-insert-and-return]]
        [clojure.java.jdbc :only [delete-rows]]
        [rssminer.test-common :only [user1 app-fixture user2
                                     mk-feeds-fixtrue]]))

(use-fixtures :each app-fixture (mk-feeds-fixtrue "test/scottgu-atom.xml"))

(def link "http://link1.com")

(deftest test-save-feeds
  (with-mysql (delete-rows :feeds ["id > 0"]))
  (save-feeds {:entries [{:link link :author "one" :simhash -1}
                         {:link link :author "two" :simhash -1}]} 1)
  (save-feeds {:entries [{:link link :author "three"}]} 2)
  (let [feeds (mysql-query ["select * from feeds"])]
    ;; should not update
    (is (empty? (filter (fn [f] (= "two" (:author f))) feeds)))
    (is (seq (filter (fn [f] (= "one" (:author f))) feeds)))
    (is (= 2 (count feeds)))))

(deftest test-save-feeds2
  (with-mysql (delete-rows :feeds ["id > 0"]))
  (save-feeds {:entries [{:link link :author "one" :simhash -1}
                         {:link link :author "two" :simhash 2}]} 1)
  (let [feeds (mysql-query ["select * from feeds"])]
    ;; should save
    (is (seq (filter (fn [f] (= "two" (:author f))) feeds)))
    (is (seq (filter (fn [f] (= "one" (:author f))) feeds)))
    (is (= 2 (count feeds)))))

(deftest test-insert-user-vote
  (let [fid (-> (mysql-query ["select id from feeds"]) first :id)]
    (insert-user-vote (:id user1) fid 1)
    (is (= 1 (-> (mysql-query
                  ["SELECT vote_user FROM user_feed WHERE
                    user_id = ? AND feed_id = ?" (:id user1) fid])
                 first :vote_user)))
    (insert-user-vote (:id user1) fid -1)
    (is (= -1 (-> (mysql-query
                   ["SELECT vote_user FROM user_feed WHERE
                    user_id = ? AND feed_id = ?" (:id user1) fid])
                  first :vote_user)))))

(deftest test-insert-read-date
  (let [fid (-> (mysql-query ["select id from feeds"]) first :id)]
    (mark-as-read (:id user1) fid)
    (is (< 1 (-> (mysql-query
                  ["SELECT read_date FROM user_feed WHERE
                    user_id = ? AND feed_id = ?" (:id user1) fid])
                 first :read_date)))))
