(ns freader.handlers.users
  (:use  [ring.util.response :only [redirect]]
         [sandbar.stateful-session :only [session-put!]])
  (:require [freader.db.user :as db]
            [freader.views.users :as view]))

(defn show-login-page [req]
  (view/login-page "/app"))

(defn login [req]
  (let [{:keys [email password return-url]} (:params req)
        user (db/authenticate email password)]
    (if user
      (do
        (session-put! :user user)
        (redirect return-url))
      (view/login-page return-url))))

(defn show-signup-page [req]
  (view/signup-page))

(defn signup [req]
  (let [{:keys [email password]} (:params req)
        user (db/create-user {:email email
                              :password password})]
    (redirect "/app")))
