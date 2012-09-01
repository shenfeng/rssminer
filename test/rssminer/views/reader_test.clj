(ns rssminer.views.reader-test
  (use clojure.test
       [rssminer.test-common :only [test-app auth-app auth-app2
                                    app-fixture]]))

(use-fixtures :each app-fixture)

(deftest test-index-page
  (let [resp (test-app {:uri "/"
                        :request-method :get})]
    (is (= 302 (:status (test-app {:uri "/a"
                                   :request-method :get}))))
    (is (= "/demo" (get-in (test-app {:uri "/"
                                      :params {"r" "d"}
                                      :request-method :get})
                           [:headers "Location"])))
    (is (= "/?r=d" (get-in (auth-app {:uri "/demo"
                                      :request-method :get})
                           [:headers "Location"])))
    (is (= "/a" (get-in (auth-app {:uri "/"
                                   :request-method :get})
                        [:headers "Location"])))
    (is (= "no-cache" ((:headers resp) "Cache-Control")))
    (is (= "text/html; charset=utf-8" ((:headers resp) "Content-Type")))
    (is (= 200 (:status (test-app {:uri "/s/js/lib/underscore.js"
                                   :request-method :get
                                   :headers {}}))))
    (is (= 200 (:status resp)))))

(deftest test-admin-recompute-scores
  (let [resp (test-app {:uri "/admin/compute"
                        :request-method :get})]
    (is (= 401 (:status resp))))
  (let [resp (auth-app2 {:uri "/admin/compute"
                         :request-method :get})]
    (is (= 401 (:status resp))))
  (let [resp (auth-app {:uri "/admin/compute"
                        :request-method :get
                        :params {"id" 1}})]
    ;; should be 401
    (is (= 401 (:status resp)))))

(deftest test-demo-page
  (let [resp (test-app {:uri "/demo"
                        :request-method :get})]
    (is (= 200 (:status resp)))))

