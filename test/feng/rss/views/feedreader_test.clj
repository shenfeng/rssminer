(ns feng.rss.views.feedreader-test
  (use clojure.test
       clojure.contrib.trace
       [feng.rss.test-common :only [test-app]]))

(deftest test-index-page
  (let [resp (test-app {:uri "/"
                        :request-method :get})]
    (is (= 200 (:status resp)))))
