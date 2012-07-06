(ns rssminer.handlers.reader
  (:use (rssminer [util :only [user-id-from-session to-int serialize-to-js]]
                  [search :only [search* search-within-subs]])
        me.shenfeng.mustache
        [clojure.java.io :only [resource]]
        [ring.util.response :only [redirect]]
        [rssminer.db.user :only [find-user-by-email find-user-by-id]])
  (:require [rssminer.config :as cfg])
  (:import rssminer.Utils))

(deftemplate landing-page (slurp (resource "templates/landing.tpl")))

(deftemplate app-page (slurp (resource "templates/app.tpl")))

(deftemplate unsupported-page (slurp (resource "templates/browser.tpl")))

(def landing-css (slurp "public/css/landing.css"))
(def app-css (slurp "public/css/app.css"))

(defn show-unsupported-page [req]
  (to-html unsupported-page {:dev (cfg/in-dev?)
                             :prod (cfg/in-prod?)
                             :css landing-css}))

(defn show-landing-page [req]
  (let [html (to-html landing-page {:dev (cfg/in-dev?)
                                    :prod (cfg/in-prod?)
                                    :css landing-css})]
    (if (cfg/demo-user? req) {:status 200
                              :session nil ;; delete cookie
                              :session-cookie-attrs {:max-age -1}
                              :body html}
        html)))

(defn show-app-page [req]
  (if (cfg/demo-user? req)
    (assoc (redirect "/") :session nil ;; delete cookie
           :session-cookie-attrs {:max-age -1})
    (let [data {:rm {:user (find-user-by-id (user-id-from-session req))
                     :no_iframe Utils/NO_IFRAME
                     :gw (-> req :params :gw) ; google import wait
                     :ge (-> req :params :ge) ; google import error
                     :reseted Utils/RESETED_DOMAINS
                     :static_server (:static-server @cfg/rssminer-conf)
                     :proxy_server (:proxy-server @cfg/rssminer-conf)}}]
      (to-html app-page {:dev (cfg/in-dev?)
                         :prod (cfg/in-prod?)
                         :css app-css
                         :data (serialize-to-js data)}))))

(defn show-demo-page [req]
  (swap! cfg/rssminer-conf assoc :demo-user
         (find-user-by-email "demo@rssminer.net"))
  (let [data {:rm {:user (:demo-user @cfg/rssminer-conf)
                   :no_iframe Utils/NO_IFRAME
                   :demo true
                   :reseted Utils/RESETED_DOMAINS
                   :static_server (:static-server @cfg/rssminer-conf)
                   :proxy_server (:proxy-server @cfg/rssminer-conf)}}]
    {:body (to-html app-page {:dev (cfg/in-dev?)
                              :prod (cfg/in-prod?)
                              :css app-css
                              :data (serialize-to-js data)})
     :status 200
     :session (:demo-user @cfg/rssminer-conf)}))

(defn search [req]
  (let [{:keys [q limit ids]} (:params req)
        uid (user-id-from-session req)
        limit (min 20 (to-int limit))]
    (if ids
      (search-within-subs q uid (clojure.string/split ids #",") limit)
      (search* q uid limit))))
