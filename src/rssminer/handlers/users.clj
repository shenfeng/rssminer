(ns rssminer.handlers.users
  (:use  [ring.util.response :only [redirect]]
         (rssminer [util :only [user-id-from-session to-int
                                md5-sum get-expire]]
                   [config :only [rssminer-conf]])
         [clojure.data.json :only [json-str read-json]])
  (:require [rssminer.db.user :as db]
            [rssminer.db.user-feed :as uf]
            [clojure.string :as str]
            [rssminer.views.users :as view]))

(defn show-login-page [req]
  (view/login-page "/a"))

(defn login [req]
  (let [{:keys [email password return-url persistent]} (:params req)
        user (db/authenticate email password)
        return-url (or return-url "/a")]
    (if user
      (assoc (redirect return-url)
        :session {:id (:id user)}
        :session-cookie-attrs {:expires (get-expire 3)})
      (view/login-page return-url))))

(defn show-signup-page [req]
  (view/signup-page))

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
  (let [user (db/find-user-by-id (user-id-from-session req))]
    (when-let [password (-> req :body :password)]
      (let [p (md5-sum (str (:email user) "+" password))]
        (db/update-user (:id user) {:password p})))
    (let [updated (merge (:conf user) (select-keys (:body req)
                                                   [:nav :expire]))]
      (db/update-user (:id user) {:conf (json-str updated)})
      {:status 204 :body nil})))

(defn summary [req]
  (let [u-id (user-id-from-session req)
        limit (min (-> req :params :limit to-int) 40)
        offset (-> req :params :offset to-int)
        data (case (-> req :params :section)
               "latest" (uf/fetch-newest u-id limit offset)
               "voted" (uf/fetch-recent-vote u-id limit offset)
               "read" (uf/fetch-recent-read u-id limit offset)
               "recommand" (uf/fetch-likest u-id limit offset))]
    (if data
      {:body data       ;; ok, just cache for half hour
       :headers {"Cache-Control" "private, max-age=1800"}}
      data))) ;; no cache


(defn google-openid [req]
  (let [spec "http://specs.openid.net/auth/2.0/identifier_select"
        host (if (= (@rssminer-conf :profile) :dev)
               "localhost:9090/" "rssminer.net/")
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
                 "&openid.return_to=http://" (str host "login/checkauth")
                 (str "&openid.realm=http://" host))]
    (redirect url)))

(defn checkauth [req]
  (if-let [email ((:params req) "openid.ext1.value.email")]
    (assoc (redirect "/a")
      :session {:id (:id (or (db/find-user {:email email})
                             (db/create-user {:email email
                                              :provider "google"})))})
    (redirect "/")))
