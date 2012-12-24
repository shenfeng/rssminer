(ns rssminer.import-test
  (:use clojure.test
        rssminer.import
        [rssminer.test-common :only [auth-app app-fixture user1]]
        [rssminer.db.subscription :only [fetch-subs]])
  (:import java.io.File
           rssminer.Utils))

(use-fixtures :each app-fixture)

(deftest test-parset-google-output
  (let [o (map bean (Utils/parseGReaderSubs
                     (slurp "test/greader-subs-list.xml")))]
    (subscribe-all (:id user1) (Utils/parseGReaderSubs
                                (slurp "test/greader-subs-list.xml")))
    ;; fixture add 1
    (is (= 84 (count (fetch-subs (:id user1)))))
    (is (every? :title o))
    (is (every? :url o))
    (is (= (count o) 83))))
