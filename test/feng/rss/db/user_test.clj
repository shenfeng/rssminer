(ns feng.rss.db.user-test
  (:use clojure.test
        feng.rss.db.user
        clojure.contrib.trace
        [feng.rss.test-util :only [postgresql-fixture]]))

(use-fixtures :each postgresql-fixture)

(deftest test-create-authenticate-user
  (let [user (create-user {:email "test@test.com"
                           :password "123456"
                           :name "test-usre"})]
    (is (authenticate "test@test.com" "123456"))))
