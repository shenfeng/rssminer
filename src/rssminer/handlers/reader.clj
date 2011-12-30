(ns rssminer.handlers.reader
  (:use [rssminer.util :only [session-get to-int]]
        [rssminer.search :only [search*]]
        [rssminer.db.subscription :only [fetch-user-subs]])
  (:require [rssminer.views.reader :as view]
            [rssminer.config :as cfg]))

(defn landing-page [req]
  (view/landing-page))

(defn app-page [req]
  (let [user (session-get req :user)
        subs (fetch-user-subs (:id user))]
    (view/app-page {:rm {:user user
                         :proxy_server (:proxy-server @cfg/rssminer-conf)
                         :subs subs}})))

(defn dashboard-page [req]
  (view/dashboard-page))

(defn search [req]
  (let [{:keys [term limit] :or {limit 20}} (:params req)]
    (search* term (to-int limit) :user-id (:id (session-get req :user)))))
