(ns rssminer.views.users-test
  (:use clojure.test
        [rssminer.db.user :only [find-by-id]]
        [rssminer.util :only [read-if-json]]
        [rssminer.database :only [mysql-query]]
        [rssminer.test-common :only [test-app app-fixture user1
                                     auth-app json-body]]))

(use-fixtures :each app-fixture)

(deftest test-signup-login-process
  (let [params {"email" "test@test.com"
                "password" "123456"}
        signup (test-app {:request-method :post
                          :params params
                          :uri "/signup"})
        login (test-app {:request-method :post
                         :params params
                         :uri "/"})
        remerber-me (test-app {:request-method :post
                               :params (assoc params
                                         "persistent" "on")
                               :uri "/"})]

    ;; (is (not (re-find #"Expires="
    ;;                   (first ((:headers login) "Set-Cookie")))))
    (is (re-find #"Max-Age="
                 (first ((:headers remerber-me) "Set-Cookie"))))
    (is (= 200 (:status (test-app {:request-method :post
                                   :params {"email" "sdfdsf"}
                                   :uri "/"}))))
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
  (doseq [section ["newest" "voted" "read"]]
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


(deftest test-submit-feedback
  (let [resp (auth-app {:uri "/api/feedback"
                        :request-method :post
                        :remote-addr "192.192.192.192"
                        :params {"email" "test@rssminer.net"
                                 "feedback" "rssminer is awesome"
                                 "refer" "/a#ip=s"}})]
    (is (= 200 (:status resp)))
    (is (mysql-query ["select * from feedback"]))))
