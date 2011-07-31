(ns freader.util
  (:use (clojure.data [json :only [json-str Write-JSON]]))
  (:require [freader.http :as http]
            [clojure.string :as str]
            [net.cgrand.enlive-html :as html])
  (:import org.apache.commons.io.IOUtils
           org.apache.commons.codec.binary.Base64
           java.util.Date
           java.sql.Timestamp
           [java.net URI]
           [java.io InputStream StringWriter PrintWriter StringReader]
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
  (let [base (URI. base)
        link (URI. link)]
    (str (.resolve base link))))

(defn extract-links [base html]
  (let [resource (html/html-resource (StringReader. html))
        links (html/select resource [:a])
        f (fn [a] {:href (resolve-url base (-> a :attrs :href))
                  :text (html/text a)})]
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

(defn download-feed-source  [url]
  (try
    (update-in (http/get url) [:body]   ;convert to string
               (fn [in] (slurp in)))
    (catch Exception e
      (prn "ERROR GET " url ": " (.getMessage e) "\n"))))

(defn serialize-to-js [data]
  (let [stats (map
               (fn [[k v]]
                 (let [e-v (str/replace (json-str v) "'" "\\'")]
                   (str "var _" (str/upper-case (name k))
                        "_ = JSON.parse('" e-v "')"))) data)
        js (concat '("<script>") stats '("</script>"))]
    (apply str js)))

(defn download-favicon [url]
  (let [icon-url (str (http/extract-host url) "/favicon.ico")]
   (try
     (let [resp (http/get icon-url)
           img (Base64/encodeBase64String
                (IOUtils/toByteArray (:body resp)))
           code (if-let [type (:Content-Type resp)]
                  (str "data:" type ";base64,")
                  "data:image/x-icon;base64,")]
       (str code img))
     (catch Exception e
       (prn "ERROR GET " icon-url ": " (.getMessage e) "\n")))))

