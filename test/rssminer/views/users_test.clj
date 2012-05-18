(ns rssminer.views.users-test
  (:use clojure.test
        [rssminer.test-common :only [test-app app-fixture
                                     auth-app json-body]]))

(use-fixtures :each app-fixture)

(deftest test-login-sign-page
  (let [resp (test-app {:uri "/login"
                        :request-method :get})
        body (apply str (:body resp))]
    (is (= 200 (:status resp)))
    (is (re-find #"name=.*email" body))
    (is (re-find #"name=.*password" body)))
  (let [resp (test-app {:uri "/signup"
                        :request-method :get})
        body (apply str (:body resp))]
    (is (= 200 (:status resp)))
    (is (re-find #"name=.*email" body))
    (is (re-find #"name=.*password" body))))

(deftest test-signup-login-process
  (let [params {"email" "test@test.com"
                "password" "123456"}
        signup (test-app {:request-method :post
                          :params params
                          :uri "/signup"})
        login (test-app {:request-method :post
                         :params params
                         :uri "/login"})
        remerber-me (test-app {:request-method :post
                               :params (assoc params
                                         "persistent" "on")
                               :uri "/login"})]
    ;; (is (not (re-find #"Expires="
    ;;                   (first ((:headers login) "Set-Cookie")))))
    (is (re-find #"Expires="
                 (first ((:headers remerber-me) "Set-Cookie"))))
    (is (= 302 (:status signup)))
    (is (= 302 (:status login)))))

(deftest test-save-pref
  (let [resp (auth-app {:uri "/api/settings"
                        :request-method :post
                        :body (json-body {:nav [:tag1 :tag1]
                                          :expire 60})})]
    (is (= 204 (:status resp)))))

(deftest test-welcome-list
  (doseq [section ["latest" "voted" "read" "recommand"]]
    (is (= 200 (:status (auth-app {:uri "/api/welcome"
                                   :request-method :get
                                   :params {"section" section
                                            "limit" 100
                                            "offset" 1}}))))))

