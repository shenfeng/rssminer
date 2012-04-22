(ns rssminer.views.dashboard-test
  (:use clojure.test
        (clojure.data [json :only [read-json json-str]])
        (rssminer [test-common :only [auth-app mysql-fixture]])))

(use-fixtures :each mysql-fixture)

(deftest test-get-settings
  (let [resp (auth-app {:uri "/stat"
                        :request-method :get})
        settings (-> resp :body read-json)]
    (is (= 200 (:status resp)))))

(comment
  (deftest test-add-modify-settings
    (let [resp (test-app {:uri "/api/dashboard/"
                          :request-method :post
                          :body (json-str {:patten "test"})})
          body (-> resp :body read-json)]
      (is (= 200 (:status resp)))
      (is (some #(= "test" %) body)))))

