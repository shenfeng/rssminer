(ns main
  (:use [clojure.contrib.command-line :only [with-command-line]]
        [freader.routes :only [start-server]]))

(defn main [& args]
  (with-command-line args
    "start server"
    [[port "Which port server will listen. Take from input->env(READER_PORT)->8000"]
     [db-host "Db host, Take from input->env(READER_DB_HOST)->localhost"]
     [db-name "Which database to use" "freader"]
     [db-user "User name for database login" "postgres"]
     [db-password "Password for database login" "123456"]
     [profile "Configuration profile (production or development)" "development"]]
    (let [jdbc-url (str "jdbc:postgresql://"
                        (or db-host
                            (get (System/getenv) "READER_DB_HOST" "localhost"))
                        "/" db-name)
          server-port (Integer. (or port
                                    (get (System/getenv) "READER_PORT" "8100")))]
      (start-server :jdbc-url jdbc-url
                    :db-user db-user
                    :db-password db-password
                    :port server-port
                    :profile (keyword profile)))))

(apply main *command-line-args*)
