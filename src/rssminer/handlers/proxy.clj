(ns rssminer.handlers.proxy
  (:use (rssminer [http :only [client]]
                  [util :only [assoc-if ignore-error]]
                  [search :only [update-index]]
                  [config :only [rssminer-conf]])
        [clojure.tools.logging :only [debug error]]
        [rssminer.db.util :only [mysql-query mysql-insert]])
  (:require [clojure.string :as str]
            [rssminer.db.feed :as db])
  (:import [rssminer.async ProxyFuture FetchFuture FaviconFuture]
           java.io.ByteArrayInputStream
           rssminer.Utils))

(defn- compute-send-header [req]
  (let [headers (:headers req)]
    (assoc-if {}
              "User-Agent" (headers "user-agent")
              "If-Modified-Since" (headers "if-modified-since")
              "Cache-Control" (headers "cache-control"))))

;;; buggy
;;; http://www.moandroid.com/?p=2020
(defn handle-proxy [req]
  (let [uri (-> req :params :u str/reverse)]
    {:status 200
     :body (ProxyFuture. uri (compute-send-header req)
                         (:proxy @rssminer-conf)
                         (fn [status h body]
                           {:status 200 :body body :headers h}))}))

(defn- rewrite-html [link html]
  (let [proxy (str (:proxy-server @rssminer-conf) "/p?u=")]
    (Utils/rewrite html link proxy)))

(def default-header {"Content-Type" "text/html; charset=utf8"
                     "Cache-Control" "public, max-age=604800"})

(defn- fetch-and-store-orginal [id link proxy? header]
  (let [cb (fn [status headers html]     ; html is minified
             (let [final-uri (get headers Utils/FINAL_URI)]
               (if (= 200 status)
                 (do (update-index id html)
                     ;; save final_link if different
                     (db/update-feed id (if (not= final-uri link)
                                          {:original html
                                           :final_link final-uri}
                                          {:original html}))
                     {:status 200
                      :headers default-header
                      :body (if proxy? (rewrite-html final-uri html) html)})
                 (do
                   (debug link status)
                   {:status 404}))))]
    {:status 200
     :body (FetchFuture. link header (:proxy @rssminer-conf) cb)}))

(defn proxy-feed [req]
  (let [{:keys [id p]} (:params req)
        header {"User-Agent" ((:headers req) "user-agent")}
        {:keys [original link]} (db/fetch-orginal id)] ; proxy
    (if original
      {:status 200
       :headers default-header
       :body (if p (rewrite-html link original) original)}
      (fetch-and-store-orginal id link p header))))

(def no-favicon {:status 404})

(defn- fetch-favicon [hostname]
  (first (mysql-query
          ["SELECT favicon, code FROM favicon WHERE hostname = ?" hostname])))

(def favicon-header {"Content-Type" "image/x-icon"
                     "Cache-Control" "public, max-age=315360000"
                     "Expires" "Thu, 31 Dec 2017 23:55:55 GMT"})

(defn- fetch-save-favicon [hostname]
  (let [cb (fn [status headers body]
             ;; body is byte array
             (ignore-error (mysql-insert :favicon {:hostname hostname
                                                   :code status
                                                   :favicon body}))
             (if (= 200 status)
               {:status 200
                :headers favicon-header
                :body (ByteArrayInputStream. body)}
               no-favicon))]
    {:status 200
     :body (FaviconFuture. hostname (:proxy @rssminer-conf) cb)}))

(defn get-favicon [req]
  (if-let [hostname (-> req :params :h str/reverse)]
    (if-let [favicon (fetch-favicon hostname)]
      (if (= 200 (:code favicon))
        {:status 200
         :headers favicon-header
         :body (ByteArrayInputStream. (:favicon favicon))}
        no-favicon)
      (fetch-save-favicon hostname))
    no-favicon))
