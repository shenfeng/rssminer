(ns rssminer.util
  (:use (clojure.data [json :only [json-str Write-JSON]])
        [clojure.pprint :only [pprint]])
  (:require [clojure.string :as str])
  (:import java.util.Date
           java.sql.Timestamp
           java.util.concurrent.ThreadFactory
           [java.net URI]
           [java.io StringWriter PrintWriter]
           [java.security NoSuchAlgorithmException MessageDigest]))

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
  (.print out (.getTime d)))

(extend Date Write-JSON
        {:write-json write-json-date})
(extend Timestamp Write-JSON
        {:write-json write-json-date})

(defn json-response
  "Construct a JSON HTTP response."
  [status body] {:status status
                 :headers {"Content-Type" "application/json; charset=utf-8"}
                 :body (json-str body)})

(defn session-get [req key] ;; for test code easy mock
  (-> req :session key))

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

(definline to-int [s]
  `(if (integer? ~s) ~s
       (Integer/parseInt ~s)))

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

(defn threadfactory [prefix]
  (let [id (atom 0)]
    (reify ThreadFactory
      (newThread [this runnable]
        (doto (Thread. runnable (str prefix "-" (swap! id inc)))
          (.setDaemon true))))))
