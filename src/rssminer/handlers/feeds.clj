(ns rssminer.handlers.feeds
  (:use (rssminer [util :only [session-get to-int assoc-if get-expire]]
                  [config :only [rssminer-conf]]
                  [search :only [update-index]]
                  [http :only [client parse-response extract-host]])
        [clojure.tools.logging :only [debug error]]
        [clojure.data.json :only [json-str]])
  (:require [rssminer.db.feed :as db]
            [rssminer.db.user-feed :as uf])
  (:import rssminer.Utils
           rssminer.async.ProxyFuture
           org.jboss.netty.handler.codec.http.HttpResponse
           java.net.URI))

(defn user-vote [req]
  (let [fid (-> req :params :feed-id to-int)
        vote (-> req :body :vote to-int)
        user (session-get req :user)]
    (uf/insert-user-vote (:id user) fid vote)
    (if (-> user :conf :updated)
      {:status 204 :body nil}
      {:status 204 :body nil
       :session {:user (assoc user :conf
                              (assoc (:conf user) :updated true))}})))

(defn mark-as-read [req]
  (let [fid (-> req :params :feed-id to-int)
        user-id (:id (session-get req :user))]
    (uf/mark-as-read user-id fid)))

(defn get-by-subscription [req]
  (let [{:keys [rss-id limit offset] :or {limit 40 offset 0}} (:params req)
        uid (:id (session-get req :user))]
    (db/fetch-by-rssid uid (to-int rss-id) (to-int limit)
                       (to-int offset))))

(defn get-by-id [req]
  (let [feed-id (-> req :params :feed-id)]
    (db/fetch-by-id feed-id)))

(def default-header {"Content-Type" "text/html; charset=utf-8"
                     "Cache-Control" "public, max-age=604800"})

(defn- compute-send-header [req]
  (let [headers (:headers req)]
    (assoc-if {"X-Forwarded-For" (:remote-addr req)}
              "User-Agent" (headers "user-agent"))))

(defn- fetch-and-store-orginal [id link header callback]
  (let [cb (fn [{:keys [resp final-link]}]
             (let [resp (parse-response resp)]
               (if (= 200 (:status resp))
                 (let [body (Utils/minfiyHtml (:body resp) final-link)]
                   (update-index id body)
                   ;; save final_link if different
                   (db/update-feed id (if (not= final-link link)
                                        {:original body
                                         :final_link final-link}
                                        {:original body}))
                   {:status 200
                    :headers default-header
                    :body (str callback "(" (json-str body) ")")})
                 (do
                   (debug link resp)
                   {:status 404}))))]
    {:status 200
     :body (ProxyFuture. client link header (:proxy @rssminer-conf) cb)}))

(defn get-orginal [req]
  (let [{:keys [id callback]} (-> req :params)
        {:keys [original link]} (db/fetch-orginal id)] ; proxy
    (if original
      {:status 200
       :headers default-header
       :body (str callback "(" (json-str original) ")")}
      (fetch-and-store-orginal id link (compute-send-header req) callback))))

