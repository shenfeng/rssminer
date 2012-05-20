(ns rssminer.util
  (:use [clojure.data.json :only [json-str Write-JSON]]
        [clojure.tools.logging :only [error info]]
        [ring.middleware.file-info :only [make-http-format]]
        [rssminer.time :only [now-seconds]]
        [clojure.pprint :only [pprint]])
  (:require [clojure.string :as str])
  (:import java.util.Date
           java.sql.Timestamp
           [java.util Locale Calendar TimeZone Date]
           java.text.SimpleDateFormat
           java.net.URI
           me.shenfeng.http.HttpUtils
           [java.io StringWriter PrintWriter StringReader]
           [java.security NoSuchAlgorithmException MessageDigest]))

(defn get-expire "get string for http expire header" [days]
  (let [^SimpleDateFormat f (make-http-format)
        c (doto (Calendar/getInstance)
            (.add Calendar/DAY_OF_YEAR days))
        d (.getTime c)]
    (.format f d)))

(defn md5-sum
  "Compute the hex MD5 sum of a string."
  [#^String str]
  (let [alg (doto (MessageDigest/getInstance "MD5")
              (.reset)
              (.update (.getBytes str)))]
    (try
      (.toString (new BigInteger 1 (.digest alg)) 16)
      (catch NoSuchAlgorithmException e
        (throw (new RuntimeException e))))))

(defn- write-json-date [^Date d ^PrintWriter out escape-unicode?]
  (.print out (int (/ (.getTime d) 1000))))

(extend Date Write-JSON
        {:write-json write-json-date})
(extend Timestamp Write-JSON
        {:write-json write-json-date})

(defn ^:dynamic session-get [req key] ;; for test code easy mock
  (-> req :session key))

(defn extract-text [html]
  (when html
    (rssminer.Utils/extractText html)))

(defn serialize-to-js [data]
  (let [stats (map
               (fn [[k v]]
                 (str "var _" (str/upper-case (name k))
                      "_ = " (json-str v) "; ")) data)
        js (concat '("<script>") stats '("</script>"))]
    (apply str js)))

(defmacro ignore-error [& body]
  `(try ~@body
        (catch Exception _#)))

(defn assoc-if [map & kvs]
  "like assoc, but drop false value"
  (let [kvs (apply concat
                   (filter #(second %) (partition 2 kvs)))]
    (if (seq kvs) (apply assoc map kvs) map)))

(defn to-int [s] (cond
                  (string? s) (Integer/parseInt s)
                  (instance? Integer s) s
                  (instance? Long s) (.intValue ^Long s)
                  :else 0))

(defn to-boolean [s] (Boolean/parseBoolean s))

(defmacro when-lets [bindings & body]
  (if (empty? bindings)
    `(do ~@body)
    `(when-let [~@(take 2 bindings)]
       (when-lets [~@(drop 2 bindings)]
                  ~@body))))

(defmacro if-lets
  ([bindings then]
     `(if-lets ~bindings ~then nil))
  ([bindings then else]
     (if (empty? bindings)
       `~then
       `(if-let [~@(take 2 bindings)]
          (if-lets [~@(drop 2 bindings)]
                   ~then ~else)
          ~else))))

(defn trace
  ([value] (trace nil value))
  ([name value]
     (println (str "TRACE" (when name (str " " name)) ": " value))
     value))

(defn tracep
  ([value] (tracep nil value))
  ([name value]
     (println (str "TRACE" (when name (str " " name)) ":"))
     (pprint value)
     value))

(defn time-since [user]                ;45 day
  (- (now-seconds) (* (or (-> user :conf :expire) 45) 3600 24)))
