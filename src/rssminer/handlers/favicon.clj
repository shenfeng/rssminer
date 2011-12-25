(ns rssminer.handlers.favicon
  (:use [rssminer.db.util :only [h2-query h2-insert]]
        [ring.util.response :only [redirect]]
        (rssminer [http :only [client]]
                  [config :only [rssminer-conf]]))
  (:import org.jboss.netty.handler.codec.http.HttpResponse
           rssminer.async.FaviconFuture
           java.io.ByteArrayInputStream))

(defn fetch-favicon [hostname]
  (first (h2-query
          ["SELECT favicon, code FROM favicon WHERE hostname = ?" hostname])))

(def default-icon "/imgs/16px-feed-icon.png")

(def headers {"Content-Type" "image/x-icon"
              "Cache-Control" "public, max-age=315360000"
              "Expires" "Thu, 31 Dec 2037 23:55:55 GMT"})

(defn fetch-save-favicon [hostname]
  {:status 200
   :body (FaviconFuture. client hostname (:proxy @rssminer-conf)
                         (fn [resp]
                           (let [code (-> resp .getStatus .getCode)
                                 data (-> resp .getContent .array)]
                             (h2-insert :favicon {:hostname hostname
                                                  :code code
                                                  :favicon data})
                             (if (= 200 code)
                               {:status 200
                                :headers headers
                                :body (ByteArrayInputStream. data)}
                               (redirect default-icon)))))})

(defn get-favicon [req]
  (if-let [hostname (-> req :params :h)]
    (if-let [favicon (fetch-favicon hostname)]
      (if (= 200 (:code favicon))
        {:status 200
         :headers headers
         :body (ByteArrayInputStream. (:favicon favicon))}
        (redirect default-icon))
      (fetch-save-favicon hostname))
    (redirect default-icon)))

