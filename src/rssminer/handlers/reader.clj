(ns rssminer.handlers.reader
  (:use (rssminer [util :only [session-get to-int]]
                  [time :only [now-seconds]]
                  [classify :only [re-compute-sysvote]]
                  [search :only [search*]])
        [ring.util.response :only [redirect]]
        [rssminer.db.subscription :only [fetch-user-subs]])
  (:require [rssminer.views.reader :as view]
            [rssminer.config :as cfg]))

(defn landing-page [req]
  (if (session-get req :user)
    (redirect "/a")
    (view/landing-page)))

(defn- time-since [user]
  (- (now-seconds) (* (or (-> user :conf :expire) 90) 3600)))

(defn app-page [req]
  (let [user (session-get req :user)
        ts (time-since user)
        resp (view/app-page {:rm {:user user
                                  :proxy_server (:proxy-server
                                                 @cfg/rssminer-conf)
                                  :subs (fetch-user-subs (:id user) ts)}})]
    (if (-> user :conf :updated)
      (do
        (re-compute-sysvote (:id user) ts)
        {:status 200
         :body resp
         :headers {"Content-Type" "text/html; charset=utf-8"}
         :session {:user (assoc user :conf
                                (dissoc (:conf user) :updated))}})
      resp)))

(defn dashboard-page [req]
  (view/dashboard-page))

(defn search [req]
  (let [{:keys [term limit] :or {limit 20}} (:params req)]
    (search* term (to-int limit) :user-id (:id (session-get req :user)))))
