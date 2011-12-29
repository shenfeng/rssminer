(ns rssminer.handlers.feeds
  (:use (rssminer [util :only [session-get to-int]]
                  [config :only [rssminer-conf]]
                  [http :only [client parse-response]])
        [clojure.tools.logging :only [debug error]])
  (:require [rssminer.db.feed :as db])
  (:import rssminer.Utils
           rssminer.async.ProxyFuture
           org.jboss.netty.handler.codec.http.HttpResponse
           java.net.URI))

(defn user-vote [req]
  (let [fid (-> req :params :feed-id to-int)
        vote (-> req :body :vote to-int)
        user-id (:id (session-get req :user))]
    (db/insert-vote user-id fid vote)))

(defn mark-as-read [req]
  (let [fid (-> req :params :feed-id to-int)
        user-id (:id (session-get req :user))]
    (db/mark-as-read user-id fid)))

(defn get-by-subscription [req]
  (let [{:keys [rss-id limit offset] :or {limit 30 offset 0}} (:params req)
        uid (:id (session-get req :user))]
    (db/fetch-by-rssid uid (to-int rss-id) (to-int limit)
                       (to-int offset))))

(defn get-by-id [req]
  (let [feed-id (-> req :params :feed-id)]
    (db/fetch-by-id feed-id)))

(defn- rewrite-html [original link proxy]
  (if proxy
    (Utils/rewrite original link (:proxy-server @rssminer-conf))
    (Utils/rewrite original link)))

(defn- fetch-and-store-orginal [id link proxy]
  {:status 200
   :body (ProxyFuture. client link {} (:proxy @rssminer-conf)
                       (fn [resp]
                         (let [resp (parse-response resp)]
                           (if (= 200 (:status resp))
                             (let [body (:body resp)]
                               (db/save-feed-original id body)
                               {:status 200
                                :headers {"Content-Type"
                                          "text/html; charset=utf-8"}
                                :body (rewrite-html body link proxy)})
                             (do
                               (debug link resp)
                               {:status 404})))))})

(defn get-orginal [req]
  (let [{:keys [id p]} (-> req :params)
        {:keys [original link]} (db/fetch-orginal id)] ; proxy
    (if original
      (rewrite-html original link p)
      (fetch-and-store-orginal id link p))))

