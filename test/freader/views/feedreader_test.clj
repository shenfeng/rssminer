(ns freader.views.feedreader-test
  (use clojure.test
       clojure.contrib.trace
       [freader.test-common :only [test-app]]))

(deftest test-index-page
  (let [resp (test-app {:uri "/"
                        :request-method :get})
        app-resp (test-app {:uri "/app"
                        :request-method :get})
        js-resp (test-app {:uri "/js/lib/jquery.js"
                           :request-method :get
                           :headers {}})]
    (is (= 302 (:status app-resp)))
    (is (= "no-cache" ((:headers resp) "Cache-Control")))
    (is (= "text/html; charset=utf-8" ((:headers resp) "Content-Type")))
    (is (= 200 (:status js-resp)))
    (is (= 200 (:status resp)))))
