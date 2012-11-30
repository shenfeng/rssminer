(ns rssminer.handlers.reader
  (:use (rssminer [util :only [md5-sum ignore-error serialize-to-js defhandler]]
                  [search :only [search*]])
        [clojure.java.io :only [resource]]
        [ring.util.response :only [redirect]])
  (:require [rssminer.config :as cfg]
            [clojure.string :as str]
            [rssminer.db.subscription :as sdb]
            [rssminer.db.user :as udb]
            [rssminer.db.feed :as db]
            [rssminer.tmpls :as tmpls])
  (:import rssminer.Utils
           rssminer.FaviconFuture))

(def landing-css (ignore-error (slurp "public/css/landing.css")))
(def app-css (ignore-error (slurp "public/css/app.css")))

(defn show-unsupported-page [req]
  (tmpls/browser {:css landing-css}))

(defhandler show-landing-page [req r mobile?]
  (if (= r "d")       ; redirect to /demo
    (redirect "/demo")
    (if (cfg/real-user? req)
      (redirect (if mobile? "/m" "/a"))
      (let [body (if mobile? (tmpls/m-landing)
                     (tmpls/landing { :css landing-css}))]
        (if (cfg/demo-user? req) {:status 200
                                  :session nil ;; delete cookie
                                  :session-cookie-attrs {:max-age -1}
                                  :body body}
            body)))))

(defhandler show-app-page [req uid gw ge mobile?]
  (if (cfg/demo-user? req)
    (assoc (redirect "/") :session nil ;; delete cookie
           :session-cookie-attrs {:max-age -1})
    (if mobile?
      (redirect "/m")
      (when-let [user (udb/find-by-id uid)]
        (tmpls/app {:css app-css
                    :req req
                    :email (:email user)
                    :md5 (-> user :email md5-sum)
                    :data (serialize-to-js
                           {:rm {:user user
                                 :gw gw  ; google import wait
                                 :ge ge  ; google import error
                                 :static_server (cfg/cfg :static-server)}})})))))

(defhandler show-demo-page [req mobile?]
  (if (cfg/real-user? req)
    (assoc (redirect "/?r=d") :session nil ;; delete cookie
           :session-cookie-attrs {:max-age -1})
    (let [user (dissoc (udb/find-by-email "demo@rssminer.net") :password)]
      (swap! cfg/rssminer-conf assoc :demo-user user)
      (if mobile?
        (assoc (redirect "/m") :session user)
        (let [data {:rm {:user user
                         :demo true
                         :static_server (:static-server @cfg/rssminer-conf)}}]
          {:body (tmpls/app {:email (:email user)
                             :req req
                             :md5 (-> user :email md5-sum)
                             :css app-css
                             :demo true
                             :data (serialize-to-js data)})
           :status 200
           :session user})))))

(defhandler search [req q limit tags authors fs offset uid]
  (search* q tags authors uid limit offset (= fs "1")))

(defhandler get-favicon [req h]
  (if-let [hostname (str/reverse h)]
    {:status 200
     :body (FaviconFuture. hostname
                           {"User-Agent" ((:headers req) "user-agent")}
                           (cfg/cfg :proxy)
                           (cfg/cfg :data-source))}
    {:status 404}))
