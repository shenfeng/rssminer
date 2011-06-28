(ns freader.main
  (:use [clojure.contrib.command-line :only [with-command-line]]
        [clojure.contrib.def :only [defnk]]
        [ring.adapter.jetty7 :only [run-jetty]]
        (freader [database :only [use-psql-database!]]
                 [search :only [use-index-writer!]]
                 [routes :only [app]]
                 [config :only [env-profile]])))

(defonce server (atom nil))

(defn- stop-server []
  (when-not (nil? @server)
    (.stop @server)
    (reset! server nil)))

(defn- start-server [& {:keys [jdbc-url db-user db-password port
                               index-path profile]}]
  {:pre [(#{:prod :dev} profile)]}
  (stop-server)
  (reset! env-profile profile)
  (use-index-writer! index-path)
  (use-psql-database! jdbc-url
                      db-user
                      db-password)
  (reset! server (run-jetty (app) {:port port :join? false})))

(defn main [& args]
  (with-command-line args
    "Start freader server"
    [[port "Port to listen. READER_PORT, 8000"]
     [db-host "Database host. READER_DB_HOST, localhost"]
     [db-name "Database name" "freader"]
     [db-user "Datebase user name" "postgres"]
     [db-password "Datebase password" "123456"]
     [index-path "Path to store Lucene index" "/tmp/feeds-index"]
     [profile "Profile (prod || dev)" "dev"]]
    (let [jdbc-url (str "jdbc:postgresql://"
                        (or db-host (get (System/getenv)
                                         "READER_DB_HOST" "localhost"))
                        "/" db-name)
          server-port (Integer.
                       (or port (get (System/getenv)
                                     "READER_PORT" "8100")))]
      (start-server :jdbc-url jdbc-url
                    :db-user db-user
                    :db-password db-password
                    :port server-port
                    :index-path index-path
                    :profile (keyword profile)))))

(apply main *command-line-args*)
