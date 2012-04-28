(ns rssminer.main
  (:gen-class)
  (:use [clojure.tools.cli :only [cli]]
        [me.shenfeng.http.server :only [run-server]]
        [clojure.tools.logging :only [info]]
        (rssminer [database :only [use-mysql-database!
                                   close-global-mysql-factory!]]
                  [search :only [use-index-writer!
                                 close-global-index-writer!]]
                  [routes :only [app]]
                  [redis :only [set-redis-client!]]
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
  (close-global-mysql-factory!)
  (close-global-index-writer!))

(defonce shutdown-hook (Thread. ^Runnable stop-server))

(defn start-server
  [{:keys [port index-path profile db-url worker fetcher-queue
           fetcher proxy fetch-size redis-host db-user
           proxy-server static-server]}]
  (stop-server)
  (.removeShutdownHook (Runtime/getRuntime) shutdown-hook)
  (.addShutdownHook (Runtime/getRuntime) shutdown-hook)
  (use-mysql-database! db-url db-user)
  (set-redis-client! redis-host)
  (swap! rssminer-conf assoc :profile profile
         :fetcher-queue fetcher-queue
         :fetch-size fetch-size
         :redis-host redis-host
         :proxy-server (if (= :dev profile)
                         (str proxy-server ":" port) proxy-server)
         :static-server (if (= :dev profile)
                          (str static-server ":" port) static-server)
         :proxy (if proxy socks-proxy Proxy/NO_PROXY))
  (reset! server (run-server (app) {:port port
                                    :thread worker}))
  (use-index-writer! index-path)
  (when fetcher (start-fetcher)))

(defn -main [& args]
  "Start rssminer server"
  (let [[options _ banner]
        (cli args
             ["-p" "--port" "Port to listen" :default 9090 :parse-fn to-int]
             ["--worker" "Http worker count" :default 2 :parse-fn to-int]
             ["--fetcher-queue" "queue size" :default 20 :parse-fn to-int]
             ["--fetch-size" "Bulk fetch size" :default 100 :parse-fn to-int]
             ["--profile" "dev or prod" :default :dev :parse-fn keyword]
             ["--redis-host" "Redis for session store"
              :default "127.0.0.1"]
             ["--proxy-server" "proxy server" :default "//127.0.0.1"]
             ["--static-server" "static server" :default "//localhost"]
             ["--db-url" "Mysql Database url"
              :default "jdbc:mysql://localhost/rssminer"]
             ["--db-user" "Mysql Database user name" :default "feng"]
             ["--index-path" "Path to store lucene index"
              :default "/var/rssminer/index"]
             ["--[no-]fetcher" "Start rss fetcher" :default false]
             ["--[no-]proxy" "Enable Socks proxy" :default true]
             ["--[no-]help" "Print this help"])]
    (when (:help options) (println banner) (System/exit 0))
    (start-server options)))
