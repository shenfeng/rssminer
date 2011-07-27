(ns freader.middleware
  (:use [freader.util :only [json-response]]
        [ring.util.response :only [redirect]]
        [ring.middleware.file-info :only [make-http-format]]
        [sandbar.stateful-session :only [session-get]]
        [compojure.core :only [GET POST DELETE PUT]])
  (:require [freader.config :as config]
            [clojure.data.json :as json])
  (:import java.text.SimpleDateFormat
           [java.util Locale Calendar TimeZone Date]
           java.io.File))

(def ^{:dynamic true} *user* nil)
(def ^{:dynamic true} *json-body* nil)

;;; Expires: Thu, 01 Dec 1994 16:00:00 GMT
(defn- get-expire "get string for http expire header" [days]
  (let [f (make-http-format)
        c (doto (Calendar/getInstance)
            (.add Calendar/DAY_OF_YEAR days))
        d (.getTime c)]
    (.format f d)))

(defn wrap-auth [handler]
  (fn [req]
    (let [user (session-get :user)]
      (if (and (not user) (= (:uri req) "/app"))
        (redirect "/login")
        (binding [*user* user]
          (handler req))))))

(defn wrap-cache-header
  "No cache for generated text|json file. Allow nginx to greedy cache others"
  [handler]
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
        ;; cache image with status code of 200
        (let [new-headers (assoc headers
                            "Cache-Control" "public, max-age=31536000"
                            ;; one year
                            "Expires" (get-expire 365))]
          (assoc resp :headers new-headers))))))

(defn wrap-ring-cookie-rewrite
  "rewrite ring-session cookie if user choose to persist the login"
  [handler]
  (fn [req]
    (let [persist? (-> req :params :persistent)
          resp (handler req)
          set-cookie ((:headers resp) "Set-Cookie" '())]
      (if-let [ring-cookie (first (filter
                                   #(re-find #"^ring" %) set-cookie))]
        (let [new-ring-cookie (if persist?
                                (str ring-cookie "; Expires="
                                     (get-expire 15) "; HttpOnly")
                                (str ring-cookie "; HttpOnly"))]
          (update-in resp [:headers] assoc "Set-Cookie"
                     (cons new-ring-cookie
                           (filter #(not (re-find #"^ring" %)) set-cookie))))
        resp))))

(defn wrap-failsafe
  "show an error page instead of a stacktrace when error happens."
  [handler]
  (fn [req]
    (try (handler req)
         (catch Exception e
           (print req)
           (.printStackTrace e)
           {:status 500 :body "Sorry, an error occured."}))))

(defn wrap-json
  [handler]
  (fn [req]
    ;; it must be json, keywordize by default
    (let [json-req-body (if-let [body (:body req)]
                          (let [body-str (cond (string? body) body
                                               :else (slurp body))]
                            (when (> (count body-str) 0)
                              (json/read-json body-str))))
          ;; binding for easy access
          resp-obj (binding [*json-body* json-req-body]
                     (try
                       (handler req)
                       (catch Exception e
                         (.printStackTrace e)
                         {:status 500
                          :message "Opps, an error occured"})))
          status (:status resp-obj)]
      (if (number? status)
        (json-response status (dissoc resp-obj :status))
        (json-response 200 resp-obj)))))

(let [f (SimpleDateFormat. "yyyy-HH-dd HH:mm:ss:SSS" Locale/CHINA)]
  (defn- current-ts-str []
    (.format f (Date.))))

(defn wrap-request-logging [handler]
  (fn [{:keys [request-method uri] :as req}]
    (let [start (System/currentTimeMillis)
          resp (handler req)
          finish (System/currentTimeMillis)
          total (- finish start)]
      (print
       (format "%s: %s %d %s (%dms)\n" (current-ts-str)
               (name request-method) (:status resp) uri total))
      (flush)
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
                (println file " changed, reload " ns-sym)
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
