(ns rssminer.main
  (:gen-class)
  (:use [clojure.tools.cli :only [cli]]
        [me.shenfeng.http.server :only [run-server]]
        [clojure.tools.logging :only [info]]
        (rssminer [database :only [use-mysql-database! close-global-mysql-factory!]]
                  [search :only [use-index-writer! close-global-index-writer!]]
                  [routes :only [app]]
                  [classify :only [start-classify-daemon!]]
                  [redis :only [set-redis-pool!]]
                  [util :only [to-int]]
                  [fetcher :only [start-fetcher stop-fetcher]]
                  [config :only [rssminer-conf socks-proxy]]))
  (import java.net.Proxy))

(defonce server (atom nil))

(defn stop-server []
  (stop-fetcher)
  (when-not (nil? @server)
    (info "shutdown Rssminer server....")
    (@server)
    (reset! server nil))
  ;; (stop-classify-daemon!) no need to stop it, run as it is
  (close-global-mysql-factory!)
  (close-global-index-writer!))

(defonce shutdown-hook (Thread. ^Runnable stop-server))

(defn start-server
  [{:keys [port index-path profile db-url worker fetcher-concurrency
           fetcher proxy fetch-size redis-host db-user events-threshold
           static-server bind-ip]}]
  (stop-server)
  (.removeShutdownHook (Runtime/getRuntime) shutdown-hook)
  (.addShutdownHook (Runtime/getRuntime) shutdown-hook)
  (use-mysql-database! db-url db-user)
  (set-redis-pool! redis-host)
  (swap! rssminer-conf assoc
         :profile profile
         :fetcher-concurrency fetcher-concurrency
         :fetch-size fetch-size
         :worker worker
         :events-threshold events-threshold
         :static-server (if (= :dev profile)
                          (str static-server ":" port) static-server)
         :proxy (if proxy socks-proxy Proxy/NO_PROXY))
  (start-classify-daemon!)
  (reset! server (run-server (app) {:port port :ip bind-ip :thread worker}))
  (use-index-writer! index-path)
  (when fetcher (start-fetcher)))

(defn -main [& args]
  (let [[options _ banner]
        (cli args
             ["-p" "--port" "Port to listen" :default 9090 :parse-fn to-int]
             ["--worker" "Http worker thread count" :default 2 :parse-fn to-int]
             ["--fetcher-concurrency" "" :default 10 :parse-fn to-int]
             ["--fetch-size" "Bulk fetch size" :default 20 :parse-fn to-int]
             ["--profile" "dev or prod" :default :dev :parse-fn keyword]
             ["--redis-host" "Redis for session store" :default "127.0.0.1"]
             ["--static-server" "static server" :default "//192.168.1.200"]
             ["--db-url" "MySQL Database url" :default "jdbc:mysql://localhost/rssminer"]
             ["--db-user" "MySQL Database user name" :default "feng"]
             ["--bind-ip" "Which ip to bind" :default "0.0.0.0"]
             ["--events-threshold"
              "How many user feed events buffered before recompute again"
              :default (int 20) :parse-fn to-int]
             ["--index-path" "Path to store lucene index" :default "/var/rssminer/index"]
             ["--[no-]fetcher" "Start rss fetcher" :default false]
             ["--[no-]proxy" "Enable Socks proxy" :default false]
             ["--[no-]help" "Print this help"])]
    (when (:help options) (println banner) (System/exit 0))
    (start-server options)))
