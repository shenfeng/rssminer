(ns rssminer.config
  (:use rssminer.util)
  (:import [java.net Proxy Proxy$Type InetSocketAddress]))

(defonce rssminer-conf (atom {:fetch-size 100}))

(def socks-proxy (Proxy. Proxy$Type/SOCKS
                         (InetSocketAddress. "127.0.0.1" 3128)))

(defn in-prod? [] (= (:profile @rssminer-conf) :prod))

(defn in-dev? [] (= (:profile @rssminer-conf) :dev))

(defn demo-user? [req]
  (when-let [user (:demo-user @rssminer-conf)]
    (= (user-id-from-session req) (:id user))))

(defn real-user? [req]
  (and (user-id-from-session req) (not (demo-user? req))))
