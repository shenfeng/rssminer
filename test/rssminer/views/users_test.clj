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
    (is (re-find #"Max-Age="
                 (first ((:headers remerber-me) "Set-Cookie"))))
    (is (= 200 (:status (test-app {:request-method :post
                                   :params {"email" "sdfdsf"}
                                   :uri "/login"}))))
    (is (= 302 (:status signup)))
    (is (= 302 (:status login)))))

(deftest test-save-pref
  (let [resp (auth-app {:uri "/api/settings"
                        :request-method :post
                        :body (json-body {:nav [:tag1 :tag1]
                                          :expire 60})})]
    (is (= 204 (:status resp)))))

(deftest test-welcome-list
  (doseq [section ["newest" "voted" "read" "recommend"]]
    (is (= 200 (:status (auth-app {:uri "/api/welcome"
                                   :request-method :get
                                   :params {"section" section
                                            "limit" 100
                                            "offset" 1}}))))))

(def qs "openid.ns=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0&openid.mode=id_res&openid.op_endpoint=https%3A%2F%2Fwww.google.com%2Faccounts%2Fo8%2Fud&openid.response_nonce=2012-08-15T03%3A21%3A08ZuErItF56tGqgdA&openid.return_to=http%3A%2F%2Frssminer.net%2Flogin%2Fcheckauth&openid.assoc_handle=AMlYA9XBjcRt5Kfs2uviw5jjDGtKbUFxGHV0LaLSEzxTsloSc_Kcw2ki&openid.signed=op_endpoint%2Cclaimed_id%2Cidentity%2Creturn_to%2Cresponse_nonce%2Cassoc_handle%2Cns.ext1%2Cext1.mode%2Cext1.type.email%2Cext1.value.email&openid.sig=eTCaO9HBZoRuyC2VcrR62PPZzEc%3D&openid.identity=https%3A%2F%2Fwww.google.com%2Faccounts%2Fo8%2Fid%3Fid%3DAItOawnZOOZgDsY7SGyRTtnLdXHv8jpkl9045OI&openid.claimed_id=https%3A%2F%2Fwww.google.com%2Faccounts%2Fo8%2Fid%3Fid%3DAItOawnZOOZgDsY7SGyRTtnLdXHv8jpkl9045OI&openid.ns.ext1=http%3A%2F%2Fopenid.net%2Fsrv%2Fax%2F1.0&openid.ext1.mode=fetch_response&openid.ext1.type.email=http%3A%2F%2Faxschema.org%2Fcontact%2Femail&openid.ext1.value.email=abc%40gmail.com")

(deftest test-login-google
  (let [resp (test-app {:uri "/login/checkauth"
                        :request-method :get})]

    (is (= "/" (get (:headers resp) "Location")))
    (is (= 302 (:status resp))))
  (let [resp (test-app {:uri "/login/checkauth"
                        :request-method :get
                        :query-string qs})]
    (is (= "/a" (get (:headers resp) "Location")))
    (is (= 302 (:status resp)))))
