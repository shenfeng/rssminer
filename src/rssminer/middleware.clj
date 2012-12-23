(ns rssminer.middleware
  (:use [rssminer.util :only [user-id-from-session json-str2]]
        [ring.util.response :only [redirect]]
        [rssminer.i18n :only [*req*]]
        [clojure.tools.logging :only [debug error info]]
        [compojure.core :only [GET POST DELETE PUT]])
  (:require [rssminer.config :as conf]
            [clojure.data.json :as json]))

(defn wrap-auth [handler]
  (fn [req]
    (let [uri (:uri req)]
      (if (user-id-from-session req)
        (handler req)
        (if (or (= uri "/a")
                (= uri "/m")
                (= uri "/import/google") ;;  login required
                (.startsWith ^String uri "/api"))
          (if (= "XMLHttpRequest" (-> req :headers (get "x-requested-with")))
            {:status 401} ;; easier for script to handle
            (redirect "/login"))
          (handler req))))))

;; "set no-cache header."
(defn wrap-cache-header [handler]
  (fn [req]
    (binding [*req* req]                ; for 118n
      (let [resp (handler req)
            headers (get resp :headers {})
            ctype (headers "Content-Type")]
        (if (and (or (not= 200 (:status resp))
                     (and ctype (re-find #"text|json" ctype)))
                 (not (get headers "Cache-Control")))
          ;; do not cache non-200; do not cache text, json, or xml.
          (let [new-headers (assoc headers "Cache-Control" "no-cache")]
            (assoc resp :headers new-headers))
          resp)))))

;; "show an error page instead of a stacktrace when error happens."
(defn wrap-failsafe [handler]
  (fn [req]
    (try (handler req)
         (catch Exception e
           (error e "error handling request" req)
           {:status 500 :body "Sorry, an error occured."}))))

(def ^{:private true} json-resp-header {"Content-Type"
                                        "application/json; charset=utf-8"})

(defn wrap-json [handler]
  (fn [req]
    ;; it must be json, keywordize by default
    (let [json-body (if-let [body (:body req)]
                      (let [body-str (slurp body)]
                        (json/read-json body-str)))
          resp (try ;; easier for js to understand if this is an 500
                 (handler (assoc req :body json-body))
                 (catch Exception e
                   (error e "api error Request: " req)
                   {:status 500
                    :body {:message "Opps, an error occured"}}))]
      (if (contains? resp :body)
        (let [r {:status (or (:status resp) 200)
                 :headers (merge json-resp-header (:headers resp))
                 :body (-> resp :body json-str2)}]
          (if (contains? resp :session)
            (assoc r :session (:session resp))
            r))
        {:status 200
         :headers json-resp-header
         :body (json-str2 resp)}))))

(defn wrap-request-logging-in-dev [handler]
  (if (= (conf/cfg :profile) :dev)
    (fn [{:keys [request-method ^String uri] :as req}]
      (when-not (or (.startsWith uri "/api/")
                    (.startsWith uri "/s/")
                    (.startsWith uri "/fav"))
        (require :reload 'rssminer.tmpls))
      (let [start (System/currentTimeMillis)
            resp (handler req)
            finish (System/currentTimeMillis)]
        (when-not (.startsWith uri "/fav")
          (info (name request-method) (:status resp)
                (if-let [qs (:query-string req)]
                  (str uri "?" qs) uri)
                (str (- finish start) "ms")))
        resp))
    handler))

(defmacro JPOST [path args handler]
  `(POST ~path ~args (wrap-json ~handler)))

(defmacro JPUT [path args handler]
  `(PUT ~path ~args (wrap-json ~handler)))

(defmacro JGET [path args handler]
  `(GET ~path ~args (wrap-json ~handler)))

(defmacro JDELETE [path args handler]
  `(DELETE ~path ~args (wrap-json ~handler)))
