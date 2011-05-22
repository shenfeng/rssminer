(ns feng.rss.views.feeds-test
  (:use clojure.test
        clojure.contrib.trace
        clojure.pprint
        [clojure.contrib.json :only [read-json json-str]]
        (feng.rss [middleware :only [*user*]]
                  [test-common :only [auth-app mock-http-get]]
                  [test-util :only [postgresql-fixture]]
                  [util :only [http-get]])))

(use-fixtures :each postgresql-fixture)

(deftest test-add-feedsource
  (binding [http-get mock-http-get]
    (let [link "http://link-to-scottgu's rss"
          req {:uri "/api/feedsource"
               :request-method :put
               :body (json-str {:link link})}
          resp (auth-app req)
          add-again (auth-app req)
          obj (-> resp :body read-json)
          ;; fetch to make sure it is inserted to database
          fetch-resp (auth-app {:uri (str "/api/feeds/" (:id obj))
                                :request-method :get
                                :params {"limit" 13}})
          fetch-obj (-> fetch-resp :body read-json)]
      (is (= 200 (:status resp)))
      (is (= 200 (:status fetch-resp)))
      (is (= 400 (:status add-again)))
      (is (= 15 (count (:items obj))))
      (is (= 13 (count (:items fetch-obj)))))))
