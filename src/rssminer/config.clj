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

(def http-proxy (Proxy. Proxy$Type/HTTP
                        (InetSocketAddress. "127.0.0.1" 3128)))

(def rssminer-agent
  "Mozilla/5.0 (compatible; Rssminer/1.0; +http://rssminer.net)")

(def no-proxy Proxy/NO_PROXY)

(def ungroup "ungrouped")

(def crawler-queue 100)

(def fetcher-queue 100)

(def fetch-size 100)

(def ignored-url-patten
  (re-pattern (str "(?i)(jpg|png|gif|css|js|jpeg|pdf|"
                   "mp3|swf|mp4|wmv|flv|rm|mov|zip|mkv|rar)$")))

(def black-domain-pattens (atom
                           (delay (db/fetch-black-domain-pattens))))

(defn black-domain? [host]
  (or (some #(re-find % host) @@black-domain-pattens)
      (not (re-find #"(com|net|me)$" host))))

(defn add-black-domain-patten [patten]
  (db/insert-black-domain-patten patten)
  (reset! black-domain-pattens (delay (db/fetch-black-domain-pattens))))

(defn reseted-url? [url]
  (some #(re-find % url) #{#"blogspot\.com"}))

(def popular-tags ["clojure" "compojure" "jquery" "jdk" "linux"
                   "database" "performance"  "java" "emacs"
                   "web" "python" "vim"])

(def multi-domains (atom
                    (delay (set (db/fetch-multi-domains)))))

(defn multi-domain? [domain]
  (@@multi-domains domain))
