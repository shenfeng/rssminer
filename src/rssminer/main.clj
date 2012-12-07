(ns rssminer.main
  (:gen-class)
  (:use [clojure.tools.cli :only [cli]]
        [me.shenfeng.http.server :only [run-server]]
        [clojure.java.shell :only [sh]]
        [clojure.tools.logging :only [info]]
        (rssminer [database :only [use-mysql-database! close-global-mysql-factory!]]
                  [search :only [use-index-writer! close-global-index-writer!]]
                  [routes :only [app]]
                  [classify :only [start-classify-daemon!]]
                  [redis :only [set-redis-pool!]]
                  [util :only [to-int]]
                  [fetcher :only [start-fetcher stop-fetcher]]
                  [config :only [rssminer-conf socks-proxy cfg]]))
  (:require [clojure.string :as str])
  (:import java.net.Proxy))

(defonce server (atom nil))

(defn stop-server []
  (stop-fetcher)
  ;; (stop-classify-daemon!) no need to stop it, run as it is
  (close-global-index-writer!)
  (when-not (nil? @server)
    (info "shutdown Rssminer server....")
    (@server)
    (reset! server nil))
  (close-global-mysql-factory!))

(defonce shutdown-hook (Thread. ^Runnable stop-server))

(defmacro do-kill-if-prod [& body]
  `(if (= :prod (cfg :profile))
     (loop [i# 1]
       (let [pid# (str/trim (:out (sh "lsof"
                                      "-t" "-sTCP:LISTEN"
                                      (str "-i:" (cfg :port)))))]
         (when-not (str/blank? pid#)
           (info "kill pid" pid# i# "times, status" (:exit (sh "kill" pid#)))))
       (let [r# (try ~@body 1
                     (catch java.net.BindException e#
                       (if (> i# 30)    ; wait about 4.5s
                         (do
                           (info "giving up")
                           (throw e#))
                         (Thread/sleep 150))))]
         (when-not r#
           (recur (inc i#)))))
     (do ~@body)))

(defn start-server []
  (stop-server)
  (.removeShutdownHook (Runtime/getRuntime) shutdown-hook)
  (.addShutdownHook (Runtime/getRuntime) shutdown-hook)
  (use-mysql-database!)
  (set-redis-pool!)
  (do-kill-if-prod
   (reset! server (run-server (app) {:port (cfg :port)
                                     :ip (cfg :bind-ip)
                                     :worker-name-prefix "w"
                                     :thread (cfg :worker)}))
   (info "server start"  (str (cfg :bind-ip) ":" (cfg :port))
         "with" (cfg :worker) "workers"))
  (use-index-writer!)
  (start-classify-daemon!)
  (when (cfg :fetcher) (start-fetcher)))

(defn -main [& args]
  (let [[options _ banner]
        (cli args
             ["-p" "--port" "Port to listen" :default 9090 :parse-fn to-int]
             ["--worker" "Http worker thread count" :default 4 :parse-fn to-int]
             ["--fetcher-concurrency" "" :default 10 :parse-fn to-int]
             ["--fetch-size" "Bulk fetch size" :default 20 :parse-fn to-int]
             ["--profile" "dev or prod" :default :dev :parse-fn keyword]
             ["--redis-host" "Redis host" :default "127.0.0.1"]
             ["--redis-port" "Redis port" :default 6379 :parse-fn to-int]
             ["--static-server" "static server" :default "//192.168.1.200"]
             ["--db-url" "MySQL Database url" :default "jdbc:mysql://localhost/rssminer"]
             ["--db-user" "MySQL user name" :default "feng"]
             ["--db-pass" "MySQL password " :default ""]
             ["--bind-ip" "Which ip to bind" :default "0.0.0.0"]
             ["--events-threshold"
              "How many user feed events buffered before recompute again"
              :default (int 20) :parse-fn to-int]
             ["--index-path" "Path to store lucene index" :default "/var/rssminer/index"]
             ["--[no-]fetcher" "Start rss fetcher" :default false]
             ["--[no-]proxy" "Enable Socks proxy" :default false]
             ["--[no-]help" "Print this help"])]
    (when (:help options) (println banner) (System/exit 0))
    (swap! rssminer-conf merge options)
    (swap! rssminer-conf assoc
           :static-server (if (= :dev (cfg :profile))
                            (str (cfg :static-server) ":" (cfg :port))
                            (cfg :static-server))
           :proxy (if (cfg :proxy) socks-proxy Proxy/NO_PROXY))
    (start-server)))
