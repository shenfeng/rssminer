(ns rssminer.handlers.users
  (:use  [ring.util.response :only [redirect]]
         [clojure.java.io :only [resource]]
         me.shenfeng.mustache
         (rssminer [util :only [user-id-from-session to-int md5-sum]]
                   [config :only [rssminer-conf]])
         [clojure.data.json :only [json-str read-json]])
  (:require [rssminer.db.user :as db]
            [rssminer.db.user-feed :as uf]
            [clojure.string :as str]))

(deftemplate login-page (slurp (resource "templates/login.tpl")))
(deftemplate signup-page (slurp (resource "templates/signup.tpl")))

(defn show-login-page [req]
  (to-html login-page {:return_url (or (-> req :params :return_url) "/a")}))

(defn show-signup-page [req] (to-html signup-page nil))

(defn login [req]
  (let [{:keys [email password return-url persistent]} (:params req)
        user (db/authenticate email password)
        return-url (or return-url "/a")]
    (if user
      (assoc (redirect return-url)
        :session {:id (:id user)}      ; IE does not persistent cookie
        :session-cookie-attrs {:max-age (* 3600 24 7)})
      (to-html login-page {:return_url return-url
                           :msg "Login failed, Email or password error"}))))

(defn logout [req]
  (assoc (redirect "/")
    :session nil ;; delete cookie
    :session-cookie-attrs {:max-age -1}))

(defn signup [req]
  (let [{:keys [email password]} (:params req)]
    (if (or (str/blank? email)
            (str/blank? password))
      (redirect "/") ;; TODO error reporting
      (let [user (db/create-user {:email email
                                  :password password})]
        (assoc (redirect "/a")           ; no conf currently
          :session {:id (:id user)})))))

;;; :nav => show and hide of left nav
;;; :expire => feed mark as read after X days
;;; :like_threshhold => more than it mean like
;;; :dislike_threshhold => less than it mean dislike
(defn save-settings [req]
  (let [uid (user-id-from-session req)]
    (when-let [password (-> req :body :password)]
      (let [user (db/find-user-by-id uid)
            p (md5-sum (str (:email user) "+" password))]
        (db/update-user uid {:password p})))
    (when-let [nav (-> req :body :nav)]
      (db/update-user uid {:conf (json-str {:nav nav})}))
    {:status 204 :body nil}))

(defn summary [req]
  (let [u-id (user-id-from-session req)
        limit (min (-> req :params :limit to-int) 40)
        offset (-> req :params :offset to-int)
        data (case (-> req :params :section)
               "newest" (uf/fetch-newest u-id limit offset)
               "voted" (uf/fetch-recent-vote u-id limit offset)
               "read" (uf/fetch-recent-read u-id limit offset)
               "recommend" (uf/fetch-likest u-id limit offset))]
    (if data
      {:body data       ;; ok, just cache for 10 miniutes
       :headers {"Cache-Control" "private, max-age=600"}}
      data))) ;; no cache


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
      :session {:id (:id (or (db/find-user-by-email email)
                             (db/create-user {:email email
                                              :provider "google"})))}
      :session-cookie-attrs {:max-age (* 3600 24 7)})
    (redirect "/")))
