(ns rssminer.classify-test
  (:use clojure.test
        [rssminer.db.feed :only [save-feeds]]
        [rssminer.database :only [mysql-db-factory]]
        [rssminer.db.util :only [mysql-query mysql-insert-and-return]]
        (rssminer [test-common :only [user1 app-fixture mk-feeds-fixtrue]]
                  [util :only [now-seconds]]))
  (:require [rssminer.db.user-feed :as uf])
  (:import rssminer.classfier.UserSysVote))

(defn prepare-insert-feeds [test-fn]
  (let [rss-id (-> (mysql-query ["SELECT rss_link_id FROM user_subscription"])
                   first :rss_link_id)]
    (save-feeds {:entries
                 [{:author "feng"
                   :title "Clean Your App Permissions in 2 Minutes"
                   :link "http://news.ycombinator.com/"
                   :published_ts (now-seconds)
                   :summary "Hackers reportedly plan to fight back against"},
                  {:author "feng"
                   :published_ts (now-seconds)
                   :title "Physicists Seek To Lose The Lecture As Teaching"
                   :link "http://www.npr.org"
                   :summary "the book didn't get written, but the TOC"}
                  {:author "feng"
                   :published_ts (now-seconds)
                   :title "CSS3 PROGRESS BARS"
                   :link "http://dipperstove.com/design/css3-progress-bars.html"
                   :summary "the book didn't get written, but the TOC"}]}
                rss-id)
    (test-fn)))

(use-fixtures :each app-fixture
              (mk-feeds-fixtrue "test/scottgu-atom.xml")
              prepare-insert-feeds)

(defn- re-compute-sysvote [user-id]
  (let [v (UserSysVote. user-id
                        0
                        (:ds @mysql-db-factory))]
    (.reCompute v)))

(deftest test-recompute-sysvote
  (let [u-id (:id user1)
        feed-ids (map :id (mysql-query ["select id from feeds"]))]
    (uf/insert-user-vote u-id (first feed-ids) 1)
    (is (not (re-compute-sysvote u-id)))
    (uf/insert-user-vote u-id (second feed-ids) -1)
    (is (re-compute-sysvote u-id))
    (re-compute-sysvote u-id)
    (let [all (mysql-query ["select * from user_feed"])]
      (is (= 4 (count all))))))
