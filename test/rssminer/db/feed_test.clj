(ns rssminer.db.feed-test
  (:use clojure.test
        rssminer.db.feed
        (rssminer [test-common :only [user1 app-fixture mk-feeds-fixtrue]])))

(use-fixtures :each app-fixture (mk-feeds-fixtrue "test/scottgu-atom.xml"))

(deftest test-fetch-for-nav
  (let [unread (fetch-unread-meta (:id user1))]
    (is (= 1 (count unread)))))
