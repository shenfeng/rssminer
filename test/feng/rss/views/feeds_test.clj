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
          req {:uri "/api/feeds"
               :request-method :put
               :body (json-str {:link link})}
          subscribe-resp (auth-app req)
          subscribe-again (auth-app req)
          subscription (-> subscribe-resp :body read-json)
          ;; fetch to make sure it is inserted to database
          fetch-resp (auth-app {:uri (str "/api/feeds/" (:id subscription))
                                :request-method :get
                                :params {"limit" 13}})
          fetched-feeds (-> fetch-resp :body read-json)]
      (is (= 200 (:status subscribe-resp)))
      (is (= 200 (:status fetch-resp)))
      (is (= 409 (:status subscribe-again)))
      (is (= 13 (count (:items fetched-feeds)))))))
