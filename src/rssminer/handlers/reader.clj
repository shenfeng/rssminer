(ns rssminer.handlers.reader
  (:use (rssminer [util :only [user-id-from-session to-int]]
                  [search :only [search* search-within-subs]])
        [rssminer.db.user :only [find-user-by-id]])
  (:require [rssminer.views.reader :as view]
            [rssminer.config :as cfg])
  (:import rssminer.Utils))

(defn landing-page [req]
  (view/landing-page))

(defn app-page [req]
  (let [uid (user-id-from-session req)]
    (view/app-page {:rm {:user (find-user-by-id uid)
                         :no_iframe Utils/NO_IFRAME
                         :gw (-> req :params :gw) ; google import wait
                         :ge (-> req :params :ge) ; google import error
                         :reseted Utils/RESETED_DOMAINS
                         :static_server (:static-server @cfg/rssminer-conf)
                         :proxy_server (:proxy-server @cfg/rssminer-conf)}})))

(defn demo-page [req]
  {:body (view/app-page
          ;; 1 is me
          {:rm {:user (assoc (find-user-by-id 1) :email "demo@rssminer.net")
                :no_iframe Utils/NO_IFRAME
                :demo true
                :reseted Utils/RESETED_DOMAINS
                :static_server (:static-server @cfg/rssminer-conf)
                :proxy_server (:proxy-server @cfg/rssminer-conf)}})
   :status 200
   :session {:id 0}})

(defn search [req]
  (let [{:keys [q limit ids]} (:params req)
        uid (user-id-from-session req)
        limit (min 20 (to-int limit))]
    (if ids
      (search-within-subs q uid (clojure.string/split ids #",") limit)
      (search* q uid limit))))
