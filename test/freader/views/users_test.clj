(ns freader.views.users-test
  (:use clojure.test
        [sandbar.stateful-session :only [session-put!]]
        clojure.contrib.mock
        (freader [test-common :only [test-app postgresql-fixture]])))

(use-fixtures :each postgresql-fixture)

(deftest test-login-sign-page
  (let [resp (test-app {:uri "/login"
                        :request-method :get})]
    (is (= 200 (:status resp)))
    (is (re-find #"name=\"email" (:body resp)))
    (is (re-find #"name=\"password" (:body resp))))
  (let [resp (test-app {:uri "/signup"
                        :request-method :get})]
    (is (= 200 (:status resp)))
    (is (re-find #"name=\"email" (:body resp)))
    (is (re-find #"name=\"password" (:body resp)))))

(deftest test-signup-login-process
  (expect [session-put! (times 1 (has-args [#(#{:user} %)])) ]
          (let [email "test@test.com"
                password "123456"
                req {:request-method :post
                     :params {"email" email
                              "password" password}}
                signup (test-app (assoc req :uri "/signup"))
                login (test-app (assoc req :uri "/login"))]
            (is (= 302 (:status signup)))
            (is (= 302 (:status login))))))
