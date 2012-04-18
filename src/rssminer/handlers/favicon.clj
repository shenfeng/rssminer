(ns rssminer.handlers.favicon
  (:use [rssminer.db.util :only [mysql-query mysql-insert]]
        [ring.util.response :only [redirect]]
        (rssminer [http :only [client]]
                  [util :only [ignore-error]]
                  [config :only [rssminer-conf]]))
  (:require [clojure.string :as str])
  (:import org.jboss.netty.handler.codec.http.HttpResponse
           rssminer.async.FaviconFuture
           java.io.ByteArrayInputStream))

(defn fetch-favicon [hostname]
  (first (mysql-query
          ["SELECT favicon, code FROM favicon WHERE hostname = ?" hostname])))

(def default-icon "/imgs/16px-feed-icon.png")

(def headers {"Content-Type" "image/x-icon"
              "Cache-Control" "public, max-age=315360000"
              "Expires" "Thu, 31 Dec 2037 23:55:55 GMT"})

(defn fetch-save-favicon [hostname]
  {:status 200
   :body (FaviconFuture. client hostname (:proxy @rssminer-conf)
                         (fn [^HttpResponse resp]
                           (let [code (-> resp .getStatus .getCode)
                                 data (-> resp .getContent .array)]
                             (ignore-error
                              (mysql-insert :favicon {:hostname hostname
                                                      :code code
                                                      :favicon data}))
                             (if (= 200 code)
                               {:status 200
                                :headers headers
                                :body (ByteArrayInputStream. data)}
                               (redirect default-icon)))))})

(defn get-favicon [req]
  (if-let [hostname (-> req :params :h str/reverse)]
    (if-let [favicon (fetch-favicon hostname)]
      (if (= 200 (:code favicon))
        {:status 200
         :headers headers
         :body (ByteArrayInputStream. (:favicon favicon))}
        (redirect default-icon))
      (fetch-save-favicon hostname))
    (redirect default-icon)))

