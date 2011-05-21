(ns feng.rss.middleware
  (:require [feng.rss.config :as config])
  (:import java.text.SimpleDateFormat
           java.util.TimeZone
           java.util.Locale
           java.util.Calendar))


(def china-timezone (TimeZone/getTimeZone "Asia/Shanghai"))

(defn- get-calendar [] (Calendar/getInstance china-timezone Locale/CHINA))

(defn- current-ts-str []
  (let [calendar (get-calendar)
        get (fn [field] (.get calendar field))]
    (format "%d-%d-%d:%d:%d:%d:%d" (get Calendar/YEAR) (get Calendar/MONTH)
            (get Calendar/DAY_OF_MONTH) (get Calendar/HOUR_OF_DAY)
            (get Calendar/MINUTE) (get Calendar/SECOND)
            (get Calendar/MILLISECOND))))

(defn- log [msg & vals]
  (let [line (apply format msg vals)]
    (println line)))

(defn wrap-request-logging [handler]
  (fn [{:keys [request-method uri] :as req}]
    (let [start (System/currentTimeMillis)
          resp (handler req)
          finish (System/currentTimeMillis)
          total (- finish start)]
      (log "%s: request %s %s (%dms)" (current-ts-str) request-method uri
           total)
      resp)))

(defn wrap-reload-in-dev [handler reload-meta]
  (if (config/in-dev?)
    (let [read-mtime (fn [dir]
                       (reduce
                         (fn [totalTime file]
                           (+ totalTime
                             (.lastModified file))) 0 (file-seq dir)))
          map-fn (fn [e]
                   (let [file (java.io.File. (key e))]
                     {file {:ns (val e)
                            :mtime (read-mtime file)}}))
          data (atom
                 (apply merge
                   (map map-fn reload-meta)))]
      (fn [request]
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
        (handler request)))
    handler))

(defn- get-expire "get string for http expire header" [days]
  (let [f (SimpleDateFormat. "EEE, dd MMM yyyy kk:mm:ss zzz" Locale/ENGLISH)
        c (doto (Calendar/getInstance)
            (.add Calendar/DAY_OF_YEAR days))
        d (.getTime c)]
    (.format f d)))

(defn wrap-cache-header
  "No cache for generated text|json file. Allow nginx to greedy cache others"
  [handler]
  (fn [request]
    (let [response (handler request)
          headers (response :headers)
          ctype (headers "Content-Type")]
      (if (or
            (not= 200 (:status response))
            (and ctype (re-find #"text|json|xml" ctype)))
        ; do not cache non-200; do not cache text, json, or xml.
        (let [new-headers (assoc headers
                            "Cache-Control" "no-cache")]
          (assoc response :headers new-headers))
        ; cache image with status code of 200
        (let [new-headers (assoc headers
                            "Cache-Control" "public, max-age=31536000"
                            ;; one year
                            "Expires" (get-expire 365))]
          (assoc response :headers new-headers))))))

