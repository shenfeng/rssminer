(ns rssminer.main
  (:gen-class)
  (:use [clojure.tools.cli :only [cli optional required]]
        [ring.adapter.netty :only [run-netty]]
        [clojure.tools.logging :only [info]]
        (rssminer [database :only [use-h2-database!
                                   close-global-h2-factory!]]
                  [search :only [use-index-writer!
                                 close-global-index-writer!]]
                  [routes :only [app]]
                  [util :only [to-int to-boolean]]
                  [fetcher :only [start-fetcher stop-fetcher]]
                  [crawler :only [start-crawler stop-crawler]]
                  [config :only [env-profile netty-option]])))

(defonce server (atom nil))

(defn stop-server []
  (stop-crawler)
  (stop-fetcher)
  (when-not (nil? @server)
    (info "shutdown netty server....")
    (@server))
  (close-global-h2-factory!)
  (close-global-index-writer!))

(defn start-server
  [{:keys [port index-path profile db-path h2-trace worker
           run-crawler auto-server run-fetcher]}]
  {:pre [(#{:prod :dev} profile)]}
  (stop-server)
  (use-h2-database! db-path :trace h2-trace :auto-server auto-server)
  (reset! env-profile profile)
  (reset! server (run-netty (app) {:port port
                                   :worker worker
                                   :netty netty-option}))
  (info "netty server start at port" port)
  (use-index-writer! index-path)
  (when run-crawler (start-crawler))
  (when run-fetcher (start-fetcher)))

(defn -main [& args]
  "Start rssminer server"
  (start-server
   (cli args
        (optional ["-p" "--port" "Port to listen" :default "8100"]
                  to-int)
        (optional ["--worker" "Http worker thread count" :default "1"]
                  to-int)
        (optional ["--profile" "dev or prod" :default "dev"] keyword)
        (optional ["--db-path" "H2 Database file path"
                   :default "/dev/shm/rssminer"])
        (optional ["--auto-server" "H2 Database Automatic Mixed Mode"
                   :default "false"] to-boolean)
        (optional ["--h2-trace" "Enable H2 trace" :default "false"]
                  to-boolean)
        (optional ["--run-crawler" "Start link crawler" :default "false"]
                  to-boolean)
        (optional ["--run-fetcher" "Start rss fetcher" :default "false"]
                  to-boolean)
        (optional ["--index-path" "Path to store lucene index"
                   :default "/dev/shm/index"]))))
