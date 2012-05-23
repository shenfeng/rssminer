(ns rssminer.handlers.proxy
  (:use (rssminer [util :only [to-int]]
                  [config :only [rssminer-conf]]))
  (:require [clojure.string :as str]
            [rssminer.db.feed :as db])
  (:import [rssminer.async ProxyFuture FeedFuture FaviconFuture]
           rssminer.Utils))

(defn- req-headers [req] {"User-Agent" ((:headers req) "user-agent")})

;;; buggy
;;; http://www.moandroid.com/?p=2020
(defn handle-proxy [req]
  (let [uri (-> req :params :u str/reverse)]
    {:status 200
     :body (ProxyFuture. uri (req-headers req) @rssminer-conf)}))

(defn proxy-feed [req]
  (let [id (-> req :params :id to-int)
        link (db/fetch-link id)]
    (if link
      {:status 200
       :body (FeedFuture. id link (req-headers req) @rssminer-conf)}
      {:status 404
       :body "Not found"})))

(defn get-favicon [req]
  (if-let [hostname (-> req :params :h str/reverse)]
    {:status 200
     :body (FaviconFuture. hostname (req-headers req) @rssminer-conf)}
    {:status 404}))
