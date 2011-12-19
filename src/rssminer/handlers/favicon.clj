(ns rssminer.handlers.favicon
  (:use [rssminer.db.util :only [h2-query h2-insert]]
        [rssminer.http :only [client]])
  (:import org.jboss.netty.handler.codec.http.HttpResponse
           java.io.ByteArrayInputStream))

(defn fetch-favicon [hostname]
  (-> (h2-query ["select favicon from favicon where hostname = ?" hostname])
      first :favicon))

(defn insert-favicon [hostname data]
  (h2-insert :favicon {:hostname hostname
                       :favicon data}))

(def headers {"Content-Type" "image/x-icon"
              "Cache-Control" "public, max-age=315360000"
              "Expires" "Thu, 31 Dec 2037 23:55:55 GMT"})

(defn download-favicon [hostname]
  (let [url (str "http://" hostname "/favicon.ico")
        ^HttpResponse resp (-> client (.execGet url) .get)]
    (if (= 200 (-> resp .getStatus .getCode))
      (let [data (-> resp .getContent .array)]
        (insert-favicon hostname data)
        {:status 200
         :headers headers
         :body (ByteArrayInputStream. data)})
      {:status 404})))

(defn get-favicon [req]
  (if-let [hostname (-> req :params :h)]
    (if-let [data (fetch-favicon hostname)]
      {:status 200
       :headers headers
       :body (ByteArrayInputStream. data)}
      (download-favicon hostname))
    {:status 404}))

