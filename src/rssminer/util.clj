(ns rssminer.util
  (:use [clojure.data.json :only [json-str Write-JSON write-json read-json]]
        [clojure.tools.logging :only [error info]]
        [ring.middleware.file-info :only [make-http-format]]
        [clojure.pprint :only [pprint]])
  (:require [clojure.string :as str])
  (:import java.util.Date
           java.sql.Timestamp
           java.net.URI
           me.shenfeng.http.HttpUtils
           [rssminer.db Feed Subscription]
           [java.io StringWriter PrintWriter StringReader]
           [java.security NoSuchAlgorithmException MessageDigest]))
;;; "Compute the hex MD5 sum of a string."
(defn md5-sum [#^String input]
  (let [alg (doto (MessageDigest/getInstance "MD5")
              (.update (.getBytes input)))
        hash (.toString (new BigInteger 1 (.digest alg)) 16)
        length (.length hash)]
    (if (> 32 length)
      ;; 0x065 => 65, leading zero is dropped by BigInteger
      (apply str (concat (repeat (- 32 length) \0) hash))
      hash)))

(defn- write-json-date [^Date d ^PrintWriter out escape-unicode?]
  (.print out (int (/ (.getTime d) 1000))))

(defn json-str2 [json] (json-str json :escape-unicode false))

(defn read-if-json [str]
  (when str (read-json str)) )

;;; 1861 req/s vs 1606 req/s compare with reflection
(defn- write-json-feed [^Feed f ^PrintWriter out escape-unicode?]
  (.print out \{)
  (.print out "\"id\":") (.print out (.getId f))
  (.print out ",\"rssid\":") (.print out (.getRssid f))
  (.print out ",\"score\":")
  (.print out (/ (long (* 100 (.getScore f))) 100.0))
  (.print out ",\"vote\":") (.print out (.getVote f))
  (.print out ",\"link\":") (write-json (.getLink f) out escape-unicode?)
  (.print out ",\"title\":") (write-json (.getTitle f) out escape-unicode?)
  (.print out ",\"author\":") (write-json (.getAuthor f) out escape-unicode?)
  (.print out ",\"tags\":") (write-json (.getTags f) out escape-unicode?)
  (.print out ",\"pts\":") (.print out (.getPublishedts f))
  (.print out ",\"readts\":") (.print out (.getReadts f))
  (when-let [s (.getSummary f)]
    (.print out ",\"summary\":") (write-json s out escape-unicode?))
  (.print out ",\"votets\":") (.print out (.getVotets f))
  (.print out \}))

(defn- write-json-sub [^Subscription f ^PrintWriter out escape-unicode?]
  (.print out (json-str2 (dissoc (bean f) :class))))

(extend Date Write-JSON
        {:write-json write-json-date})
(extend Timestamp Write-JSON
        {:write-json write-json-date})
(extend Feed Write-JSON
        {:write-json write-json-feed})
(extend Subscription Write-JSON
        {:write-json write-json-sub})

(defn ^:dynamic user-id-from-session [req] ;; for test code easy mock
  (if (= {} (:session req)) ; ring return empty map if session is null
    nil (:session req)))

(definline now-seconds [] `(quot (System/currentTimeMillis) 1000))

(defn serialize-to-js [data]
  (let [stats (map
               (fn [[k v]]
                 (str "var _" (str/upper-case (name k))
                      "_ = " (json-str v) "; ")) data)]
    (apply str stats)))

(defmacro ignore-error [& body]
  `(try ~@body (catch Exception _#)))

;; "like assoc, but drop false value"

(defn assoc-if [map & kvs]
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

(defn valid-url? [url] (ignore-error (.getHost (URI/create url))))

(defn mobile? [req]
  (when-let [ua (get-in req  [:headers "user-agent"])]
    (re-find #"Android|iPhone" ua)))

(defmacro defhandler [handler bindings & body]
  (let [req (bindings 0)
        bindings (rest bindings)
        ks (map (fn [s] (keyword (name s))) bindings)
        vals (map (fn [k]
                    (cond (= :limit k) `(min 30 (to-int (or (~k (:params ~req)) 20)))
                          (= :offset k) `(to-int (or (~k (:params ~req)) 0))
                          (= :rss-id k) `(to-int (~k (:params ~req)))
                          (= :uid k) `(user-id-from-session ~req)
                          (= :mobile? k) `(mobile? ~req)
                          :else `(~k (:params ~req)))) ks)]
    `(defn ~handler [~req]
       (let [~@(interleave bindings vals)]
         ~@body))))


(comment
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

  ;;from clojure/contrib/strint.clj: author Chas Emerick
  (defn- silent-read [s]
    (try
      (let [r (-> s java.io.StringReader. java.io.PushbackReader.)]
        [(read r) (slurp r)])
      ;; this indicates an invalid form -- the head of s is just string data
      (catch Exception e )))

  (defn- interpolate
    ([s atom?]
       (if-let [[form rest] (silent-read (subs s (if atom? 2 1)))]
         (cons form (interpolate (if atom? (subs rest 1) rest)))
         (cons (subs s 0 2) (interpolate (subs s 2)))))
    ([^String s]
       (if-let [start (->> ["~{" "~("]
                           (map #(.indexOf s ^String %))
                           (remove #(== -1 %))
                           sort
                           first)]
         (let [f (subs s 0 start)
               rst (interpolate (subs s start) (= \{ (.charAt s (inc start))))]
           (if (> (count f) 0)
             (cons f rst)
             rst))
         (if (> (count s) 0) [s] []))))

  (comment (let [a 3] (<< "a: ~{a}; inc: ~(inc a)")))
  (defmacro << [string] `(str ~@(interpolate string))))
