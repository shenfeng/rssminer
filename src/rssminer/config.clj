(ns rssminer.config
  (:use rssminer.util)
  (:import [java.net Proxy Proxy$Type InetSocketAddress]))

(defonce rssminer-conf (atom {:fetch-size 100
                              :profile :test
                              :redis-port 6379
                              ;; :user-agent "Mozilla/5.0 (X11; Linux x86_64; rv:10.0.9) Gecko/20100101 Firefox/10.0.9 Iceweasel/10.0.9"
                              :user-agent "Mozilla/5.0 (compatible; Rssminer/1.0; +http://rssminer.net)"
                              :static-server "//127.0.0.1"
                              :redis-host "127.0.0.1"
                              :proxy Proxy/NO_PROXY}))

(def socks-proxy (Proxy. Proxy$Type/SOCKS
                         (InetSocketAddress. "127.0.0.1" 3128)))

(defn demo-user? [req]
  (when-let [user (:demo-user @rssminer-conf)]
    (= (user-id-from-session req) (:id user))))

(def cache-control {"Cache-Control" "private, max-age=600"})

(defn real-user? [req]
  (and (user-id-from-session req) (not (demo-user? req))))

(defn cfg [key & [default]]
  (if-let [v (or (key @rssminer-conf) default)]
    v
    (when-not (contains? @rssminer-conf key)
      (throw (RuntimeException. (str "unknow config for key " (name key)))))))
