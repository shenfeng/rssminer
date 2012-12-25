(ns rssminer.handlers.reader
  (:use (rssminer [util :only [md5-sum json-str2 defhandler]]
                  [search :only [search*]])
        [clojure.java.io :only [resource]]
        [ring.util.response :only [redirect]])
  (:require [rssminer.config :as cfg]
            [clojure.string :as str]
            [rssminer.db.subscription :as sdb]
            [rssminer.db.user :as udb]
            [rssminer.db.feed :as db]
            [rssminer.tmpls :as tmpls])
  (:import rssminer.FaviconFuture))

(defn show-unsupported-page [req]
  (tmpls/browser))

(defhandler landing-page [req r mobile? return-url]
  (if (= r "d")       ; redirect to /demo
    (redirect "/demo")
    (if (cfg/real-user? req)
      (redirect (if mobile? "/m" "/a"))
      (let [body (if mobile? (tmpls/m-landing)
                     (tmpls/landing {:return-url return-url}))]
        (if (cfg/demo-user? req) {:status 200
                                  :session nil ;; delete cookie
                                  :session-cookie-attrs {:max-age -1}
                                  :body body}
            body)))))

(defn- get-subs [uid]
  (filter (fn [s]
            (and (:title s) (> (:total s) 0)))
          (sdb/fetch-subs uid)))

(defhandler show-app-page [req uid gw ge mobile?]
  (if (cfg/demo-user? req)
    (assoc (redirect "/") :session nil ;; delete cookie
           :session-cookie-attrs {:max-age -1})
    (if mobile?
      (redirect "/m")
      (when-let [user (udb/find-by-id uid)]
        (tmpls/app {:email (:email user)
                    :md5 (-> user :email md5-sum)
                    :data (json-str2 {:user user
                                      :subs (get-subs uid)
                                      :gw gw      ; google import wait
                                      :ge ge      ; google import error
                                      :static_server (cfg/cfg :static-server)})})))))

(defhandler show-demo-page [req mobile?]
  (if (cfg/real-user? req)
    (assoc (redirect "/?r=d") :session nil ;; delete cookie
           :session-cookie-attrs {:max-age -1})
    (let [user (cfg/cfg :demo-user)]
      (if mobile?
        (assoc (redirect "/m") :session user)
        {:body (tmpls/app {:email (:email user)
                           :md5 (-> user :email md5-sum)
                           :demo true
                           :data (json-str2 {:user user
                                             :subs (get-subs (:id user))
                                             :demo true
                                             :static_server (cfg/cfg :static-server)})})
         :status 200
         :session user}))))

(defhandler search [req q limit tags authors fs offset uid]
  (search* q tags authors uid limit offset (= fs "1")))

(defhandler get-favicon [req h]
  (if (get-in req [:headers "if-modified-since"])
    {:status 304}
    (if-let [hostname (str/reverse h)]
      {:status 200
       :body (FaviconFuture. hostname
                             {"User-Agent" ((:headers req) "user-agent")}
                             (cfg/cfg :proxy)
                             (cfg/cfg :data-source))}
      {:status 404})))

(defhandler save-feedback [req uid email feedback refer]
  (udb/save-feedback {:email email
                      :ip (:remote-addr req)
                      :feedback feedback
                      :refer refer
                      :user_id uid})
  "ok")
