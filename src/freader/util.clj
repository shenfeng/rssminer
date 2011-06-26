(ns freader.util
  (:use (clojure.contrib [json :only [json-str Write-JSON]]))
  (:require [freader.http :as http]
            [clojure.string :as str])
  (:import java.io.PrintWriter
           java.text.SimpleDateFormat
           org.apache.commons.io.IOUtils
           org.apache.commons.codec.binary.Base64
           java.util.Date
           java.sql.Timestamp
           [java.io InputStream StringWriter]
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

(defn download-feed-source  [url]
  (try
    (update-in (http/get url) [:body]
               (fn [in]
                 (slurp in)))
    (catch Exception e)))

(defn download-favicon [url]
  (try
    (let [resp (http/get
                (str (http/extract-host url) "/favicon.ico"))
          img (Base64/encodeBase64String
               (IOUtils/toByteArray (:body resp)))
          code (if-let [type (:Content-Type resp)]
                 (str "data:" type ";base64,")
                 "data:image/x-icon;base64,")]
      (str code img))
    (catch Exception e)))

