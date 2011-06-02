(ns feng.rss.views.feeds-test
  (:use clojure.test
        clojure.contrib.trace
        clojure.pprint
        [clojure.contrib.json :only [read-json json-str]]
        (feng.rss [middleware :only [*user*]]
                  [test-common :only [auth-app mock-http-get]]
                  [test-util :only [postgresql-fixture]]
                  [util :only [http-get get-favicon]])))

(use-fixtures :each postgresql-fixture)

(deftest test-add-feedsource
  (binding [http-get mock-http-get
            get-favicon (fn [link] nil)]
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
          fetch-all (auth-app {:uri "/api/feeds"
                               :request-method :get})
          fetch-obj (-> fetch-resp :body read-json)]
      (is (= 200 (:status resp)))
      (is (= 200 (:status fetch-resp)))
      (is (= 200 (:status fetch-all)))
      (is (= 400 (:status add-again)))
      (is (= 15 (count (:items obj))))
      (is (= 13 (count (:items fetch-obj)))))))
