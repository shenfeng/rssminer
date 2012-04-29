(ns rssminer.handlers.reader
  (:use (rssminer [util :only [session-get to-int time-since]]
                  [time :only [now-seconds]]
                  [database :only [mysql-db-factory]]
                  [search :only [search*]])
        [ring.util.response :only [redirect]]
        [rssminer.db.subscription :only [fetch-user-subs]]
        [clojure.data.json :only [read-json]]
        [rssminer.db.user :only [fetch-conf]])
  (:require [rssminer.views.reader :as view]
            [rssminer.config :as cfg])
  (:import rssminer.classfier.UserSysVote))

(defn landing-page [req]
  (view/landing-page))

(defn- recompute-if-needed [updated user ts]
  (or (and updated (let [v (UserSysVote. (:id user)
                                         ts
                                         (:ds @mysql-db-factory))
                         result (.reCompute v)]
                     (when result
                       [(aget result 0) (aget result 1)])))
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
                                    :static_server (:static-server
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
