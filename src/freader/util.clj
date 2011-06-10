(ns freader.util
  (:use (clojure.contrib [json :only [json-str Write-JSON]]
                         [base64 :only [encode *base64-alphabet*]]))
  (:require  [clj-http.client :as http])
  (:import java.io.PrintWriter
           java.text.SimpleDateFormat
           [java.net URL URI]
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

(let [f (SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss Z")]
  (defn- write-json-date [d ^PrintWriter out]
    (.print out (str \" (.format f d) \"))))

(extend java.util.Date Write-JSON
        {:write-json write-json-date})
(extend java.sql.Timestamp Write-JSON
        {:write-json write-json-date})

(defn json-response
  "Construct a JSON HTTP response."
  [status body] {:status status
                 :headers {"Content-Type" "application/json; charset=utf-8"}
                 :body (json-str body)})

(defn http-get
  ([url] (http-get url {}))
  ([uri req] (try
               (http/request (merge req  {:method :get :url uri}))
               (catch Exception e))))

(defn get-host [host]
  (let [uri (URI. host)
        port (if (= -1 (.getPort uri)) ""
                 (str ":" (.getPort uri)))
        schema (.getScheme uri)
        host (.getHost uri)]
    (str schema "://" host port)))

(defn download-favicon [url]
  (let [url (URL. url)
        output (StringWriter.)
        in (.. url openConnection getInputStream)]
    (encode in output *base64-alphabet* nil)
    (str "data:image/x-icon;base64," (.toString output))))

(defn get-favicon [host]
  (let [url (str (get-host host) "/favicon.ico")]
    (try (download-favicon url)
         (catch Exception e nil))))
