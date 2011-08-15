(ns rssminer.config
  (:use [rssminer.db.config :as db])
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

(def crawler-threads-count 10)

(def fetch-size 150)

(defn rand-ts [] (rand-int 1000000))


(def black-domain-pattens (atom nil))

(defn- init-domain-patterns []
  (when (nil? @black-domain-pattens)
    (reset! black-domain-pattens
            (db/fetch-black-domain-pattens))))

(defn black-domain? [host]
  (init-domain-patterns)
  (some #(re-find % host) @black-domain-pattens))

(defn add-black-domain-patten [patten]
  (db/insert-black-domain-patten patten)
  (swap! black-domain-pattens conj (re-pattern patten)))


(def reseted-hosts (atom nil))

(defn- init-reseted-hosts []
  (when (nil? @reseted-hosts)
    (reset! reseted-hosts
            (db/fetch-reseted-domain-pattens))))

(defn reseted-url? [url]
  (init-reseted-hosts)
  (some #(re-find % url) @reseted-hosts))

(defn add-reseted-domain [domain]
  (db/insert-reseted-domain-patten domain)
  (swap! reseted-hosts conj (re-pattern domain)))
