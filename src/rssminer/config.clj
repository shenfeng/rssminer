(ns rssminer.config
  (:use [rssminer.db.config :as db]
        [rssminer.util :only [assoc-if]])
  (:import [java.net Proxy Proxy$Type InetSocketAddress]))

(defonce env-profile (atom :dev))

(defn in-prod? []
  (= @env-profile :prod))

(defn in-dev? []
  (= @env-profile :dev))

(def netty-option {"receiveBufferSize" 16384
                   "sendBufferSize" 32768
                   "child.receiveBufferSize" 16384
                   "child.sendBufferSize" 32768
                   "reuseAddress" true
                   "child.reuseAddress" true
                   "tcpNoDelay" true
                   "child.tcpNoDelay" true
                   "child.keepAlive" false
                   "child.connectTimeoutMillis" 4000})

(def socks-proxy (Proxy. Proxy$Type/SOCKS
                         (InetSocketAddress. "localhost" 3128)))

(def rssminer-agent
  "Mozilla/5.0 (compatible; Rssminer/1.0; +http://rssminer.net)")

(def no-proxy Proxy/NO_PROXY)

(def ungroup "ungrouped")

(def crawler-queue 100)

(def fetcher-threads-count 2)

(def fetch-size 150)

(defn rand-ts [] (rand-int 1000000))

(def ignored-url-patten
  (re-pattern (str "(?i)(jpg|png|gif|css|js|jpeg|pdf|"
                   "mp3|swf|mp4|wmv|flv|rm|mov|zip|mkv|rar)$")))

(def non-url-patten #"(?i)^\s*(javascript|mailto|#)")

(def black-domain-pattens (atom
                           (delay (db/fetch-black-domain-pattens))))

(defn black-domain? [host]
  (or (some #(re-find % host) @@black-domain-pattens)
      (not (re-find #"(com|net|me)$" host))))

(defn add-black-domain-patten [patten]
  (db/insert-black-domain-patten patten)
  (reset! black-domain-pattens (delay (db/fetch-black-domain-pattens))))

(def reseted-hosts (atom
                    (delay (db/fetch-reseted-domain-pattens))))

(defn reseted-url? [url]
  (some #(re-find % url) @@reseted-hosts))

(defn add-reseted-domain [domain]
  (db/insert-reseted-domain-patten domain)
  (reset! reseted-hosts (delay (db/fetch-reseted-domain-pattens))))

(def popular-tags ["clojure" "compojure" "jquery" "jdk" "linux"
                   "database" "performance"  "java" "emacs"
                   "web" "python" "vim"])

(def multi-domains (atom
                    (delay (set (db/fetch-multi-domains)))))

(defn multi-domain? [domain]
  (@@multi-domains domain))
