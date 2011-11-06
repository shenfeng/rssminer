(ns rssminer.views.feedreader-test
  (use clojure.test
       [rssminer.test-common :only [test-app auth-app app-fixture]]))

(use-fixtures :each app-fixture)

(deftest test-index-page
  (let [resp (test-app {:uri "/"
                        :request-method :get})
        app-resp (test-app {:uri "/app"
                            :request-method :get})
        js-resp (test-app {:uri "/js/lib/jquery.js"
                           :request-method :get
                           :headers {}})]
    (is (= 200 (:status app-resp)))
    (is (= "no-cache" ((:headers resp) "Cache-Control")))
    (is (= "text/html; charset=utf-8" ((:headers resp) "Content-Type")))
    (is (= 200 (:status js-resp)))
    (is (= 200 (:status resp)))))

(deftest test-get-reader-page
  (let [resp (auth-app {:uri "/app"
                        :request-method :get})]
    (is (= 200 (:status resp)))))

(deftest test-dashboar-page
  (let [resp (test-app {:uri "/dashboard"
                        :request-method :get})]
    (is (= 302 (:status resp)))))
