(ns rssminer.handlers.reader
  (:use (rssminer [util :only [session-get to-int time-since]]
                  [time :only [now-seconds]]
                  [database :only [mysql-db-factory]]
                  [search :only [search*]])
        [ring.util.response :only [redirect]]
        [rssminer.db.subscription :only [fetch-user-subs fetch-user-subids]]
        [clojure.data.json :only [read-json]]
        [rssminer.db.user :only [fetch-conf]])
  (:require [rssminer.views.reader :as view]
            [rssminer.config :as cfg]
            [clojure.string :as str])
  (:import rssminer.Utils))

(defn landing-page [req]
  (view/landing-page))

(defn app-page [req]
  (let [user (session-get req :user)]
    (view/app-page {:rm {:user user
                         :no_iframe Utils/NO_IFRAME
                         :reseted Utils/RESETED_DOMAINS
                         :static_server (:static-server @cfg/rssminer-conf)
                         :proxy_server (:proxy-server @cfg/rssminer-conf)}})))

(defn dashboard-page [req]
  (view/dashboard-page))

(defn search [req]
  (let [{:keys [q limit ids]} (:params req)
        user-id (:id (session-get req :user))
        limit (min 20 (to-int limit))]
    (if ids
      (search* q user-id (map to-int (str/split ids #",")) limit)
      (search* q user-id (fetch-user-subids user-id) limit))))
