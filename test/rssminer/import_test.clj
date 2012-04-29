(ns rssminer.import-test
  (:use clojure.test
        rssminer.import
        [rssminer.test-common :only [auth-app app-fixture user1]]
        [rssminer.db.subscription :only [fetch-user-subs]])
  (:import java.io.File
           rssminer.importer.Parser))

(use-fixtures :each app-fixture)

(def ^File opml (File. "test/opml.xml"))

(deftest test-parse-opml
  (let [result (map bean (Parser/parseOPML (slurp opml)))]
    (is (= 53 (count result)))
    (is (every? :title result))
    (is (every? :url result))))

(deftest test-ompl-import
  (let [resp (auth-app
              {:uri "/api/import/opml"
               :request-method :post
               :params {"file" {"filename" (.getName opml)
                                "size" (.length opml)
                                "content-type" "text/xml"
                                "tempfile" opml}}})]
    (is (= 52 (count (fetch-user-subs (:id user1) (* 100 3600) 1.0 0))))
    (is (= 200 (:status resp)))))

(deftest test-parset-google-output
  (let [o (map bean (Parser/parseGReaderSubs
                     (slurp "test/greader-subs-list.xml")))]
    (subscribe-all (:id user1) (Parser/parseGReaderSubs
                                (slurp "test/greader-subs-list.xml")))
    (is (= 83 (count (fetch-user-subs (:id user1) (* 100 3600) 1.0 0))))
    (is (every? :title o))
    (is (every? :url o))
    (is (= (count o) 83))))
