(ns feng.rss.views.feeds-test
  (:use clojure.test
        clojure.contrib.trace
        clojure.pprint
        [clojure.contrib.json :only [read-json json-str]]
        (feng.rss [middleware :only [*user*]]
                  [test-common :only [auth-app mock-http-get]]
                  [test-util :only [postgresql-fixture]]
                  [util :only [http-get]]
                  )))

(use-fixtures :each postgresql-fixture)

(deftest test-add-feedsource
  (binding [http-get mock-http-get]
    (let [resp (auth-app {:uri "/api/feedsource"
                          :request-method :put
                          :body (json-str {:link "http://lin-to-scottgu's rss"})})
          obj (-> resp :body read-json)
          fetch-resp (auth-app {:uri (str "/api/feeds/" (:id obj))
                           :request-method :get})
          fetch-obj (-> fetch-resp :body read-json)]
      (is (= 200 (:status resp)))
      (is (= 200 (:status fetch-resp)))
      (is (= 15 (count (:items obj))))
      (is (= 10 (count (:items fetch-obj)))))))
