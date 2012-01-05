(ns rssminer.handlers.reader
  (:use (rssminer [util :only [session-get to-int]]
                  [time :only [now-seconds]]
                  [classify :only [re-compute-sysvote]]
                  [search :only [search*]])
        [ring.util.response :only [redirect]]
        [rssminer.db.subscription :only [fetch-user-subs]]
        [clojure.data.json :only [read-json]]
        [rssminer.db.user :only [fetch-conf]])
  (:require [rssminer.views.reader :as view]
            [rssminer.config :as cfg]))

(defn landing-page [req]
  (if (session-get req :user)
    (redirect "/a")
    (view/landing-page)))

(defn- time-since [user]                ;45 day
  (- (now-seconds) (* (or (-> user :conf :expire) 45) 3600 24)))

(defn- recompute-if-needed [updated user ts]
  (or (and updated (re-compute-sysvote (:id user) ts))
      (let [like (-> user :conf :like_score)
            neutral (-> user :conf :neutral_score)]
        (when (and like neutral)
          [like neutral]))
      [1.0 0]))

(defn app-page [req]
  (let [user (session-get req :user)
        ts (time-since user)
        updated (-> user :conf :updated)]
    (let [[like neutral] (recompute-if-needed updated user ts)
          new-user (if updated (assoc user :conf
                                      (assoc (dissoc (:conf user) :updated)
                                        :like_score like
                                        :neutral_score neutral))
                       user)
          resp (view/app-page {:rm {:user new-user
                                    :proxy_server (:proxy-server
                                                   @cfg/rssminer-conf)
                                    :subs (fetch-user-subs (:id user) ts
                                                           like neutral)}})]
      (if updated
        {:status 200
         :body resp
         :headers {"Content-Type" "text/html; charset=utf-8"}
         :session {:user new-user}}
        resp))))

(defn dashboard-page [req]
  (view/dashboard-page))

(defn search [req]
  (let [{:keys [term limit] :or {limit 20}} (:params req)]
    (search* term (to-int limit) :user-id (:id (session-get req :user)))))