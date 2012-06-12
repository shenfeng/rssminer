(ns rssminer.handlers.reader
  (:use (rssminer [util :only [user-id-from-session to-int]]
                  [search :only [search* search-within-subs]])
        [rssminer.db.user :only [find-user-by-email find-user-by-id]]
        [rssminer.views.layouts :only [app-page landing-page]])
  (:require [rssminer.config :as cfg])
  (:import rssminer.Utils))

(defn show-landing-page [req] (landing-page))

(defn show-app-page [req]
  (let [uid (user-id-from-session req)]
    (app-page {:rm {:user (find-user-by-id uid)
                    :no_iframe Utils/NO_IFRAME
                    :gw (-> req :params :gw) ; google import wait
                    :ge (-> req :params :ge) ; google import error
                    :reseted Utils/RESETED_DOMAINS
                    :static_server (:static-server @cfg/rssminer-conf)
                    :proxy_server (:proxy-server @cfg/rssminer-conf)}})))

(defn show-demo-page [req]
  (let [user (find-user-by-email "demo@rssminer.net")]
    {:body (app-page {:rm {:user (dissoc user :password)
                           :no_iframe Utils/NO_IFRAME
                           :demo true
                           :reseted Utils/RESETED_DOMAINS
                           :static_server (:static-server @cfg/rssminer-conf)
                           :proxy_server (:proxy-server @cfg/rssminer-conf)}})
     :status 200
     :session user}))

(defn search [req]
  (let [{:keys [q limit ids]} (:params req)
        uid (user-id-from-session req)
        limit (min 20 (to-int limit))]
    (if ids
      (search-within-subs q uid (clojure.string/split ids #",") limit)
      (search* q uid limit))))
