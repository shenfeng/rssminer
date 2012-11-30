(ns rssminer.views.users-test
  (:use clojure.test
        [rssminer.db.user :only [find-by-id]]
        [rssminer.util :only [read-if-json]]
        [rssminer.test-common :only [test-app app-fixture user1
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
    (is (re-find #"Max-Age="
                 (first ((:headers remerber-me) "Set-Cookie"))))
    (is (= 200 (:status (test-app {:request-method :post
                                   :params {"email" "sdfdsf"}
                                   :uri "/login"}))))
    (is (= 302 (:status signup)))
    (is (= 302 (:status login)))))

(deftest test-save-settings
  (let [conf {:nav ["tag1" "tag2"]
              :pref_sort "likest"}
        resp (auth-app {:uri "/api/settings"
                        :request-method :post
                        :body (json-body conf)})]
    (is (= 204 (:status resp)))
    (is (= conf (-> user1 :id find-by-id :conf read-if-json)))))

(deftest test-welcome-list
  (doseq [section ["newest" "voted" "read" "recommend"]]
    (is (= 200 (:status (auth-app {:uri "/api/welcome"
                                   :request-method :get
                                   :params {"section" section
                                            "limit" 100
                                            "offset" 1}}))))))

(deftest test-login-google
  (let [resp (test-app {:uri "/login/checkauth"
                        :request-method :get})]

    (is (= "/" (get (:headers resp) "Location")))
    (is (= 302 (:status resp))))
  (let [resp (test-app {:uri "/login/checkauth"
                        :request-method :get
                        :query-string "openid.ext1.value.email=abc%40gmail.com"})]
    (is (= "/a" (get (:headers resp) "Location")))
    (is (= 302 (:status resp)))))
