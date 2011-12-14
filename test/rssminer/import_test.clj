(ns rssminer.import-test
  (:use clojure.test
        rssminer.import
        [rssminer.test-common :only [auth-app h2-fixture]]
        [rssminer.handlers.subscriptions :only [add-subscription*]])
  (:import java.io.File
           rssminer.importer.Parser))

(use-fixtures :each h2-fixture)

(def ^File opml (File. "test/opml.xml"))

(deftest test-parse-opml
  (let [result (map bean (Parser/parseOPML (slurp opml)))]
    (is (= 53 (count result)))
    (is (every? :title result))
    (is (every? :url result))))

(deftest test-ompl-import
  (binding [add-subscription* (fn [& args])]
    (let [resp (auth-app
                {:uri "/api/import/opml-import"
                 :request-method :post
                 :params {"file" {"filename" (.getName opml)
                                  "size" (.length opml)
                                  "content-type" "text/xml"
                                  "tempfile" opml}}})]
      (is (= 200 (:status resp))))))

(deftest test-parset-google-output
  (let [o (map bean (Parser/parseGReaderSubs
                     (slurp "test/greader-subs-list.xml")))]
    (is (every? :title o))
    (is (every? :url o))
    (is (= (count o) 83))))
