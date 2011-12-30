(ns rssminer.middleware
  (:use [rssminer.util :only [session-get]]
        [ring.util.response :only [redirect]]
        [clojure.tools.logging :only [debug error]]
        [compojure.core :only [GET POST DELETE PUT]]
        [clojure.data.json :only [json-str]])
  (:require [rssminer.config :as conf]
            [clojure.data.json :as json])
  (:import java.io.File))

(defn wrap-auth [handler]
  (fn [req]
    (let [user (session-get req :user)]
      (if (and (not user) (= (:uri req) "/dashboard"))
        (redirect "/login")
        (handler req)))))

(defn wrap-cache-header
  "set no-cache header." [handler]
  (fn [req]
    (let [resp (handler req)
          headers (resp :headers)
          ctype (headers "Content-Type")]
      (if (or (not= 200 (:status resp))
              (and ctype (re-find #"text|json|xml" ctype)))
        ;; do not cache non-200; do not cache text, json, or xml.
        (let [new-headers (assoc headers
                            "Cache-Control" "no-cache")]
          (assoc resp :headers new-headers))
        resp))))

(defn wrap-failsafe
  "show an error page instead of a stacktrace when error happens."
  [handler]
  (fn [req]
    (try (handler req)
         (catch Exception e
           (error e "error handling request" req)
           {:status 500 :body "Sorry, an error occured."}))))

(defn wrap-json
  [handler]
  (fn [req]
    ;; it must be json, keywordize by default
    (let [json-req-body (if-let [body (:body req)]
                          (let [body-str (if (string? body) body
                                             (slurp body))]
                            (when (seq body-str)
                              (json/read-json body-str))))
          resp (try ;; easier for js to understand if this is an 500
                 (handler (assoc req
                            :body json-req-body))
                 (catch Exception e
                   (error e "api error\n Request: " req)
                   {:status 500
                    :body {:message "Opps, an error occured"}}))]
      (update-in (merge {:status 200
                         :headers {"Content-Type"
                                   "application/json; charset=utf-8"}}
                        (if (contains? resp :body) resp {:body resp}))
                 [:body] json-str))))

(defn wrap-request-logging-in-dev [handler]
  (if (conf/in-dev?)
    (fn [{:keys [request-method uri] :as req}]
      (let [start (System/currentTimeMillis)
            resp (handler req)
            finish (System/currentTimeMillis)]
        (debug (name request-method) (:status resp) uri
               (str (- finish start) "ms"))
        resp))
    handler))

(defn wrap-reload-in-dev [handler reload-meta]
  (if (conf/in-dev?)
    (let [read-mtime (fn [dir]
                       (reduce
                        (fn [totalTime file]
                          (+ totalTime
                             (.lastModified ^File file))) 0 (file-seq dir)))
          map-fn (fn [e]
                   (let [file (File. ^String (key e))]
                     {file {:ns (val e)
                            :mtime (read-mtime file)}}))
          data (atom
                (apply merge
                       (map map-fn reload-meta)))]
      (fn [req]
        (doseq [d @data]
          (let [file (key d)
                clj-ns (-> d val :ns)
                mtime (read-mtime file)]
            (when (> mtime (-> d val :mtime))
              (doseq [ns-sym clj-ns]
                (debug file "changed, reload" ns-sym)
                (require :reload ns-sym)
                (swap! data assoc file {:ns clj-ns
                                        :mtime mtime})))))
        (handler req)))
    handler))

(defmacro JPOST [path args handler]
  `(POST ~path ~args (wrap-json ~handler)))

(defmacro JPUT [path args handler]
  `(PUT ~path ~args (wrap-json ~handler)))

(defmacro JGET [path args handler]
  `(GET ~path ~args (wrap-json ~handler)))

(defmacro JDELETE [path args handler]
  `(DELETE ~path ~args (wrap-json ~handler)))
