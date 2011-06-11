(ns freader.views.feedreader-test
  (use clojure.test
       clojure.contrib.trace
       [freader.test-common :only [test-app]]))

(deftest test-index-page
  (let [resp (test-app {:uri "/"
                        :request-method :get})]
    (is (= 302 (:status resp)))))
