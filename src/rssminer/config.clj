(ns rssminer.config
  (:import [java.net Proxy Proxy$Type InetSocketAddress]))

(defonce rssminer-conf (atom {:fetch-size 100}))

(def socks-proxy (Proxy. Proxy$Type/SOCKS
                         (InetSocketAddress. "127.0.0.1" 3128)))

(defn in-prod? [] (= (:profile @rssminer-conf) :prod))

(defn in-dev? [] (= (:profile @rssminer-conf) :dev))

(def rssminer-agent
  "Mozilla/5.0 (compatible; Rssminer/1.0; +http://rssminer.net)")
