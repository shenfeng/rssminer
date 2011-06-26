(ns freader.import-test
  (:use clojure.test
        clojure.contrib.mock
        freader.import
        [freader.test-common :only [auth-app]]
        [freader.test-util :only [postgresql-fixture]]
        [freader.handlers.subscriptions :only [add-subscription*]]))

(use-fixtures :each postgresql-fixture)

(def opml (java.io.File. "test/opml.xml"))
(def n 52) ;; opml.xml has 52 subscriptions

(deftest test-parse-opml
  (let [result (parse-opml (slurp opml))]
    (is (= n (count result)))
    (are [d] (-> result first)
         :title
         :link
         :group_name
         :type)))

(deftest test-ompl-import
  (expect [add-subscription* (times n)]
          (let [resp (auth-app
                      {:uri "/api/import/opml-import"
                       :request-method :post
                       :params {"file" {:filename (.getName opml)
                                        :size (.length opml)
                                        :content-type "text/xml"
                                        :tempfile opml}}})]
            (is (= 200 (:status resp))))))
