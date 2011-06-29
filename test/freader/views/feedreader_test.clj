(ns freader.views.feedreader-test
  (use clojure.test
       clojure.contrib.trace
       [freader.test-common :only [test-app]]))

(deftest test-index-page
  (let [resp (test-app {:uri "/"
                        :request-method :get})
        js-resp (test-app {:uri "/js/lib/jquery-1.6.1.js"
                           :request-method :get
                           :headers {}})]
    (is (= 200 (:status js-resp)))
    (is (= 302 (:status resp)))))
