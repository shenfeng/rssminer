(ns freader.main
  (:use [clojure.tools.cli :only [cli optional required]]
        [ring.adapter.jetty7 :only [run-jetty]]
        (freader [database :only [use-psql-database!
                                  use-h2-database!]]
                 [search :only [use-index-writer!]]
                 [routes :only [app]]
                 [crawler :only [start-crawler]]
                 [config :only [env-profile]])))

(defonce server (atom nil))
(defonce crawler (atom nil))

(defn- stop-server []
  (when-not (nil? @server)
    (.stop @server)
    (reset! server nil))
  (when-not (nil? @crawler)
    (@crawler :shutdown-wait)
    (reset! crawler nil)))

(defn- start-server [{:keys [db-host db-user db-password db-name port
                             index-path profile]}]
  {:pre [(#{:prod :dev} profile)]}
  (stop-server)
  (reset! env-profile profile)
  (use-index-writer! index-path)
  (use-psql-database! (str "jdbc:postgresql://" db-host "/" db-name)
                      db-user
                      db-password)
  (use-h2-database! "/tmp/test/freader_test;TRACE_LEVEL_FILE=2")
  (reset! crawler (start-crawler))
  (reset! server (run-jetty (app) {:port port :join? false})))

(defn main [& args]
  "Start freader server"
  (start-server
   (cli args
        (optional ["-p" "--port" "Port to listen (READER_PORT || 8100)"]
                  #(Integer.
                    (or % (get (System/getenv) "READER_PORT" "8100"))))
        (optional ["--profile" "profile (dev || prod)" :default "dev"]
                  keyword)
        (optional ["--db-host" "Database host (READER_DB_HOST || localhost)"]
                  #(or % (get (System/getenv) "READER_DB_HOST" "localhost")))
        (optional ["--db-name" "Database name" :default "freader"])
        (optional ["--db-user" "Database user name" :default "postgres"])
        (optional ["--db-password" "Database password" :default "123456"])
        (optional ["--index-path" "Path to store lucene index"
                   :default "/tmp/feeds-index"]))))
