(ns rssminer.main
  (:use [clojure.tools.cli :only [cli optional required]]
        [ring.adapter.netty :only [run-netty]]
        [clojure.tools.logging :only [info]]
        (rssminer [database :only [use-h2-database!
                                   close-global-h2-factory!]]
                  [search :only [use-index-writer!
                                 close-global-index-writer!]]
                  [routes :only [app]]
                  [crawler :only [start-crawler]]
                  [config :only [env-profile]])))

(defonce server (atom nil))
(defonce crawler (atom nil))

(defn stop-server []
  (when-not (nil? @crawler)
    (info "shutdown link crawler....")
    (@crawler :shutdown)
    (reset! crawler nil))
  (when-not (nil? @server)
    (info "shutdown netty server....")
    (@server)
    (reset! server nil))
  (close-global-h2-factory!)
  (close-global-index-writer!))

(defn start-server
  [{:keys [port index-path profile db-path trace run-crawler]}]
  {:pre [(#{:prod :dev} profile)]}
  (stop-server)
  (reset! env-profile profile)
  (reset! server (run-netty (app) {:port port}))
  (info "netty server start at port" port)
  (use-index-writer! index-path)
  (use-h2-database! db-path :trace trace)
  (when run-crawler
    (reset! crawler (start-crawler))
    (info "link crawler started")))

(defn main [& args]
  "Start rssminer server"
  (start-server
   (cli args
        (optional ["-p" "--port" "Port to listen" :default "8100"]
                  #(Integer/parseInt %))
        (optional ["--profile" "profile (dev || prod)" :default "dev"]
                  keyword)
        (optional ["--db-path" "H2 Database path"
                   :default "/dev/shm/rssminer"])
        (optional ["--trace" "Enable H2 trace" :default "true"]
                  #(Boolean/parseBoolean %))
        (optional ["--run-crawler" "Start rss crawler" :default "true"]
                  #(Boolean/parseBoolean %))
        (optional ["--index-path" "Path to store lucene index"
                   :default "/dev/shm/rssminer-index"]))))
