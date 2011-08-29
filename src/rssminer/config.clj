(ns rssminer.config
  (:use [rssminer.db.config :as db]
        [rssminer.time :only [now-seconds]])
  (:import [java.net Proxy Proxy$Type InetSocketAddress]))

(defonce env-profile (atom :dev))

(defn in-prod? []
  (= @env-profile :prod))

(defn in-dev? []
  (= @env-profile :dev))

(def socks-proxy (Proxy. Proxy$Type/SOCKS
                         (InetSocketAddress. "localhost" 3128)))

(def no-proxy Proxy/NO_PROXY)

(def ungroup "ungrouped")

(def crawler-threads-count 15)

(def fetcher-threads-count 2)

(defn next-check [last-interval success?]
  (let [interval (if success?
                   (max 5400 (int (/ last-interval 1.2)))
                   (min (int (* last-interval 1.2)) (* 3600 24 20)))]
    {:check_interval interval
     :next_check_ts (+ (now-seconds) interval)}))

(def fetch-size 150)

(defn rand-ts [] (rand-int 1000000))

(def ignored-url-patten #"(?i)(jpg|png|gif|css|js|jpeg|pdf|mp3)$")

(def non-url-patten #"(?i)^\s*(javascript|mailto|#)")

(def black-domain-pattens (atom
                           (delay (db/fetch-black-domain-pattens))))

(defn black-domain? [host]
  (some #(re-find % host) @@black-domain-pattens))

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

(def popular-tags ["clojure" "web" "compojure" "jquery" "java" "emacs"
                   "jdk" "science" "community" "enterprise" "linux"
                   "virtualization" "uncategorized" "database" "performance"
                   "mysql" "web" "ruby" "php" "software" "vim"])
