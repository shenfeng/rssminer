(ns rssminer.main
  (:use [clojure.tools.cli :only [cli optional required]]
        [ring.adapter.netty :only [run-netty]]
        [clojure.tools.logging :only [info]]
        (rssminer [database :only [use-h2-database!
                                   close-global-h2-factory!]]
                  [search :only [use-index-writer!
                                 close-global-index-writer!]]
                  [routes :only [app]]
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
  [{:keys [port index-path profile db-path h2-trace
           run-crawler auto-server run-fetcher]}]
  {:pre [(#{:prod :dev} profile)]}
  (stop-server)
  (use-h2-database! db-path :trace h2-trace :auto-server auto-server)
  (reset! env-profile profile)
  (reset! server (run-netty (app) {:port port
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
                  #(Integer/parseInt %))
        (optional ["--profile" "dev or prod" :default "dev"] keyword)
        (optional ["--db-path" "H2 Database file path"
                   :default "/var/rssminer/rssminer"])
        (optional ["--auto-server" "H2 Database Automatic Mixed Mode"
                   :default "false"] #(Boolean/parseBoolean %))
        (optional ["--h2-trace" "Enable H2 trace" :default "false"]
                  #(Boolean/parseBoolean %))
        (optional ["--run-crawler" "Start link crawler" :default "false"]
                  #(Boolean/parseBoolean %))
        (optional ["--run-fetcher" "Start rss fetcher" :default "false"]
                  #(Boolean/parseBoolean %))
        (optional ["--index-path" "Path to store lucene index"
                   :default "/var/rssminer/index"]))))
