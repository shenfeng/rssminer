(ns rssminer.db.user-test
  (:use clojure.test
        rssminer.db.user
        (rssminer [test-common :only [app-fixture]])))

(use-fixtures :each app-fixture)

(deftest test-create-authenticate-user
  (let [user (create-user {:email "test@test.com"
                           :password "123456"
                           :name "test-usre"})]
    (is (authenticate "test@test.com" "123456"))))

