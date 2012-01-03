(ns rssminer.handlers.users
  (:use  [ring.util.response :only [redirect]]
         (rssminer [time :only [now-seconds]]
                   [util :only [session-get assoc-if]])
         [ring.middleware.file-info :only [make-http-format]]
         [clojure.data.json :only [json-str read-json]])
  (:require [rssminer.db.user :as db]
            [rssminer.db.user-feed :as uf]
            [rssminer.views.users :as view])
  (:import [java.util Locale Calendar TimeZone Date]
           java.text.SimpleDateFormat))

(defn- get-expire "get string for http expire header" [days]
  (let [^SimpleDateFormat f (make-http-format)
        c (doto (Calendar/getInstance)
            (.add Calendar/DAY_OF_YEAR days))
        d (.getTime c)]
    (.format f d)))

(defn show-login-page [req]
  (view/login-page "/a"))

(defn login [req]
  (let [{:keys [email password return-url persistent]} (:params req)
        user (db/authenticate email password)
        conf (when-let [conf (:conf user)] (read-json conf))
        return-url (or return-url "/a")]
    (if user
      (assoc (redirect return-url)
        :session {:user (select-keys (assoc user :conf conf)
                                     [:id :email :name :conf])}
        :session-cookie-attrs (if persistent
                                {:expires (get-expire 7)
                                 :http-only true}
                                {:http-only true}))
      (view/login-page return-url))))

(defn show-signup-page [req]
  (view/signup-page))

(defn signup [req]
  (let [{:keys [email password]} (:params req)
        user (db/create-user {:email email
                              :added_ts (now-seconds)
                              :password password})]
    (assoc (redirect "/a")              ; no conf currently
      :session {:user (select-keys user [:id :email :name])})))

;;; :nav => show and hide of left nav
;;; :height => bottom feed list height
;;; :width => nav width
;;; :expire => feed mark as read after X days
;;; :like_threshhold => more than it mean like
;;; :dislike_threshhold => less than it mean dislike
(defn save-pref [req]
  (let [user (session-get req :user)
        updated (merge
                 (:conf user)
                 (select-keys (:body req) [:nav :height :width :expire]))]
    (db/update-user (:id user) {:conf (json-str updated)})
    {:status 204
     :body nil
     :session {:user (assoc user :conf updated)}}))

(defn welcome-list [req]
  (let [u-id (:id (session-get req :user))]
    {:readed (uf/fetch-recent-read u-id 20)
     :voted (uf/fetch-recent-voted u-id 20)
     :recommend (uf/fetch-system-voteup u-id 20)}))
