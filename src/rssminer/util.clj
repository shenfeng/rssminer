(ns rssminer.util
  (:use [clojure.data.json :only [json-str Write-JSON]]
        [clojure.tools.logging :only [error info]]
        [rssminer.time :only [now-seconds]]
        [clojure.pprint :only [pprint]])
  (:require [clojure.string :as str])
  (:import java.util.Date
           java.sql.Timestamp
           java.util.concurrent.ThreadFactory
           java.net.URI
           [java.util.concurrent Executors TimeUnit ]
           [java.io StringWriter PrintWriter StringReader]
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
  (.print out (int (/ (.getTime d) 1000))))

(extend Date Write-JSON
        {:write-json write-json-date})
(extend Timestamp Write-JSON
        {:write-json write-json-date})

(defn json-response
  "Construct a JSON HTTP response."
  [status body] {:status status
                 :headers {"Content-Type" "application/json; charset=utf-8"}
                 :body (json-str body)})

(defn ^:dynamic session-get [req key] ;; for test code easy mock
  (-> req :session key))

(defn extract-text [html]
  (when html
    (rssminer.Utils/extractText html)))

(defn gen-snippet [content]
  (when content
    (rssminer.Utils/genSnippet content 280)))

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

(defn next-check [last-interval status headers]
  (if-let [location (headers "Location")]
    {:url location :domain nil :next_check_ts (rand-int 100000)}
    (let [interval (if (= 200 status)
                     (max 5400 (int (/ last-interval 1.2)))
                     (min (int (* last-interval 1.2)) (* 3600 24 20)))]
      {:check_interval interval
       :next_check_ts (+ (now-seconds) interval)})))

