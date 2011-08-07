(ns freader.middleware
  (:use [freader.util :only [json-response]]
        [ring.util.response :only [redirect]]
        [sandbar.stateful-session :only [session-get]]
        [clojure.tools.logging :only [info error]]
        [compojure.core :only [GET POST DELETE PUT]])
  (:require [freader.config :as config]
            [clojure.data.json :as json])
  (:import java.io.File))

(def ^{:dynamic true} *user* nil)
(def ^{:dynamic true} *json-body* nil)

(defn wrap-auth [handler]
  (fn [req]
    (let [user (session-get :user)]
      (if (and (not user) (= (:uri req) "/app"))
        (redirect "/login")
        (binding [*user* user]
          (handler req))))))

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
          ;; binding for easy access
          resp-obj (binding [*json-body* json-req-body]
                     (try ;; easier for js to understand if this is an 500
                       (handler req)
                       (catch Exception e
                         (error e "api error\n Request: " req)
                         {:status 500
                          :message "Opps, an error occured"})))
          status (:status resp-obj)]
      (if (number? status)
        (json-response status (dissoc resp-obj :status))
        (json-response 200 resp-obj)))))

(defn wrap-request-logging [handler]
  (fn [{:keys [request-method uri] :as req}]
    (let [start (System/currentTimeMillis)
          resp (handler req)
          finish (System/currentTimeMillis)]
      (info (name request-method) (:status resp) uri
            (str (- finish start) "ms"))
      resp)))

(defn wrap-reload-in-dev [handler reload-meta]
  (if (config/in-dev?)
    (let [read-mtime (fn [dir]
                       (reduce
                        (fn [totalTime file]
                          (+ totalTime
                             (.lastModified file))) 0 (file-seq dir)))
          map-fn (fn [e]
                   (let [file (File. (key e))]
                     {file {:ns (val e)
                            :mtime (read-mtime file)}}))
          data (atom
                (apply merge
                       (map map-fn reload-meta)))]
      (fn [req]
        (doseq [d @data]
          (let [file (key d)
                clj-ns (-> d val :ns)
                last-mtime (-> d val :mtime)
                mtime (read-mtime file)]
            (when (> mtime last-mtime)
              (doseq [ns-sym clj-ns]
                (info file "changed, reload" ns-sym)
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
