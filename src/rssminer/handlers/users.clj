(ns rssminer.handlers.users
  (:use  [ring.util.response :only [redirect]]
         [clojure.java.io :only [resource]]
         (rssminer [util :only [user-id-from-session to-int md5-sum
                                json-str2 read-if-json defhandler]]
                   [config :only [rssminer-conf cache-control]]))
  (:require [rssminer.db.user :as db]
            [rssminer.db.feed :as fdb]
            [rssminer.tmpls :as tmpls]
            [clojure.string :as str]))

(def cookie-attr {:max-age (* 3600 24 60)})

(defhandler show-login-page [req return-url]
  (tmpls/login {:return-url (or return-url "/a")}))

(defn show-signup-page [req] (tmpls/signup))

(defhandler login [req email password return-url persistent mobile?]
  (let [user (db/authenticate email password)
        return-url (or return-url "/a")]
    (if user
      (assoc (redirect return-url)
        :session {:id (:id user)}      ; IE does not persistent cookie
        :session-cookie-attrs cookie-attr)
      (if mobile?
        (tmpls/m-landing {:return-url return-url
                          :msg "Login failed, Email or password error"})
        (tmpls/login {:return-url return-url
                      :msg "Login failed, Email or password error"})))))

(defn logout [req]
  (assoc (redirect "/")
    :session nil ;; delete cookie
    :session-cookie-attrs {:max-age -1}))

(defhandler signup [req email password]
  (if (or (str/blank? email)
          (str/blank? password))
    (redirect "/") ;; TODO error reporting
    (let [user (db/create-user {:email email
                                :password password})]
      (assoc (redirect "/a")           ; no conf currently
        :session {:id (:id user)}))))

(defhandler signup [req email password]
  (if (or (str/blank? email)
          (str/blank? password))
    (redirect "/") ;; TODO error reporting
    (let [user (db/create-user {:email email
                                :password password})]
      (assoc (redirect "/a")           ; no conf currently
        :session {:id (:id user)}))))

(defn- update-conf [uid req key]
  (when-let [data (-> req :body key)]
    (let [conf (merge (-> uid db/find-by-id :conf read-if-json)
                      {key data})]
      (db/update-user uid {:conf (json-str2 conf)}))))

(defhandler save-settings [req uid]
  (when-let [password (-> req :body :password)]
    (let [user (db/find-by-id uid)
          p (md5-sum (str (:email user) "+" password))]
      (db/update-user uid {:password p})))
  (update-conf uid req :nav)
  (update-conf uid req :pref_sort)
  {:status 204 :body nil})

;;; :nav => show and hide of left nav
;;; :pref_sort => show recommand or newest
(defhandler save-settings [req uid]
  (when-let [password (-> req :body :password)]
    (let [user (db/find-by-id uid)
          p (md5-sum (str (:email user) "+" password))]
      (db/update-user uid {:password p})))
  (update-conf uid req :nav)
  (update-conf uid req :pref_sort)
  {:status 204 :body nil})


(defhandler summary [req limit offset section uid]
  (let [data (case section
               "newest" (fdb/fetch-newest uid limit offset)
               "voted" (fdb/fetch-vote uid limit offset)
               "read" (fdb/fetch-read uid limit offset)
               "recommend" (fdb/fetch-likest uid limit offset))]
    (if (and (seq data) (not= "read" section) (not= "voted" section))
      {:body data ;; ok, just cache for 10 miniutes
       :headers cache-control}
      data)))

(defn google-openid [req]
  (let [spec "http://specs.openid.net/auth/2.0/identifier_select"
        url (str "https://www.google.com/accounts/o8/ud"
                 "?openid.ns=http://specs.openid.net/auth/2.0"
                 "&openid.ns.pape=http://specs.openid.net/extensions/pape/1.0"
                 "&openid.ns.max_auth_age=300"
                 "&openid.claimed_id=" spec
                 "&openid.identity=" spec
                 "&openid.mode=checkid_setup"
                 "&openid.ui.ns=http://specs.openid.net/extensions/ui/1.0"
                 "&openid.ui.mode=popup"
                 "&openid.ui.icon=true"
                 "&openid.ns.ax=http://openid.net/srv/ax/1.0"
                 "&openid.ax.mode=fetch_request"
                 "&openid.ax.type.email=http://axschema.org/contact/email"
                 "&openid.ax.required=email"
                 "&openid.return_to=http://rssminer.net/login/checkauth"
                 "&openid.realm=http://rssminer.net/")]
    (redirect url)))

(defn checkauth [req]
  (if-let [email ((:params req) "openid.ext1.value.email")]
    (assoc (redirect "/a")
      :session (or (db/find-by-email email)
                   (db/create-user {:email email
                                    :provider "google"}))
      :session-cookie-attrs cookie-attr)
    (redirect "/")))
