(ns rssminer.util
  (:use (clojure.data [json :only [json-str Write-JSON]])
        [clojure.pprint :only [pprint]])
  (:require [rssminer.http :as http]
            [clojure.string :as str]
            [net.cgrand.enlive-html :as html])
  (:import java.util.Date
           java.sql.Timestamp
           [java.net URI]
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

(defn resolve-url [base link]
  (let [base (URI. (if (= base (http/extract-host base))
                     (str base "/")
                     base))
        link (URI. link)]
    (str (.resolve base link))))

(defn extract-links [base html]
  (let [resource (html/html-resource (StringReader. html))
        links (html/select resource [:a])
        f (fn [a] {:href (resolve-url base (-> a :attrs :href))
                  :title (html/text a)})]
    {:rss (map #(select-keys (:attrs %) [:href :title])
                     (html/select resource
                                  [(html/attr= :type "application/rss+xml")]))
     :links (map f links)}))

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

(defn serialize-to-js [data]
  (let [stats (map
               (fn [[k v]]
                 (str "var _" (str/upper-case (name k))
                      "_ = " (json-str v) "; ")) data)
        js (concat '("<script>") stats '("</script>"))]
    (apply str js)))

(definline to-int [s]
  `(if (integer? ~s) ~s
       (Integer/parseInt ~s)))

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
