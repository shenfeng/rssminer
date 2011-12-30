(ns rssminer.main
  (:gen-class)
  (:use [clojure.tools.cli :only [cli]]
        [ring.adapter.netty :only [run-netty]]
        [clojure.tools.logging :only [info]]
        (rssminer [database :only [use-h2-database!
                                   close-global-h2-factory!]]
                  [search :only [use-index-writer!
                                 close-global-index-writer!]]
                  [routes :only [app]]
                  [redis :only [set-redis-client]]
                  [util :only [to-int]]
                  [fetcher :only [start-fetcher stop-fetcher]]
                  [crawler :only [start-crawler stop-crawler]]
                  [config :only [rssminer-conf netty-option socks-proxy]]))
  (import java.net.Proxy))

(defonce server (atom nil))

(defn stop-server []
  (stop-crawler)
  (stop-fetcher)
  (when-not (nil? @server)
    (info "shutdown netty server....")
    (@server)
    (reset! server nil))
  (close-global-h2-factory!)
  (close-global-index-writer!))

(defonce shutdown-hook (Thread. ^Runnable stop-server))

(defn start-server
  [{:keys [port index-path profile db-path h2-trace worker crawler-queue
           fetcher-queue crawler fetcher proxy dns fetch-size redis-host
           proxy-server]}]
  (stop-server)
  (.removeShutdownHook (Runtime/getRuntime) shutdown-hook)
  (.addShutdownHook (Runtime/getRuntime) shutdown-hook)
  (use-h2-database! db-path :trace h2-trace)
  (set-redis-client redis-host)
  (swap! rssminer-conf assoc :profile profile
         :crawler-queue crawler-queue
         :fetcher-queue fetcher-queue
         :fetch-size fetch-size
         :redis-host redis-host
         :proxy-server (if (= :dev profile)
                         (str proxy-server ":" port) proxy-server)
         :dns-prefetch dns
         :proxy (if proxy socks-proxy Proxy/NO_PROXY))
  (reset! server (run-netty (app) {:port port
                                   :worker worker
                                   :netty netty-option}))
  (info "netty server start at port" port)
  (use-index-writer! index-path)
  (when crawler (start-crawler))
  (when fetcher (start-fetcher)))

(defn -main [& args]
  "Start rssminer server"
  (let [[options _ banner]
        (cli args
             ["-p" "--port" "Port to listen" :default 9090 :parse-fn to-int]
             ["--worker" "Http worker count" :default 1 :parse-fn to-int]
             ["--crawler-queue" "queue size" :default 200 :parse-fn to-int]
             ["--fetcher-queue" "queue size" :default 20 :parse-fn to-int]
             ["--fetch-size" "Bulk fetch size" :default 100 :parse-fn to-int]
             ["--profile" "dev or prod" :default :dev :parse-fn keyword]
             ["--redis-host" "redis for session store"
              :default "127.0.0.1"]
             ["--proxy-server" "proxy server" :default "//127.0.0.1"]
             ["--db-path" "H2 Database file path"
              :default "/var/rssminer/rssminer"]
             ["--index-path" "Path to store lucene index"
              :default "/var/rssminer/index"]
             ["--[no-]h2-trace" "Enable H2 trace"]
             ["--[no-]crawler" "Start link crawler"]
             ["--[no-]fetcher" "Start rss fetcher"]
             ["--[no-]dns" "Enable dns prefetch" :default false]
             ["--[no-]proxy" "Enable Socks proxy" :default true]
             ["--[no-]help" "Print this help"])]
    (when (:help options) (println banner) (System/exit 0))
    (start-server options)))
