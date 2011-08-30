(ns rssminer.setup-database
  (:use (rssminer [database :only [use-h2-database! close-global-h2-factory!
                                   import-h2-schema!]]
                  [util :only [session-get]]
                  [routes :only [app]])
        (rssminer.db [user :only [create-user]])
        [clojure.tools.logging :only [info]]
        [clojure.tools.cli :only [cli optional required]])
  (:import java.io.File))

(defn setup [{:keys [index-path db-path password]}]
  (info "delete" db-path
        (.delete (File. (str db-path ".h2.db"))))
  (doall (map #(info "delete" % (.delete %))
              (reverse (file-seq (File. index-path)))))
  (use-h2-database! db-path)
  (info "import h2 schema, create user feng")
  (import-h2-schema!)
  (create-user {:name "feng" :password password :email "shenedu@gmail.com"})
  (close-global-h2-factory!))

(defn main [& args]
  "Setup rssminer database"
  (setup
   (cli args
        (required ["-p" "--password" "password"])
        (optional ["--db-path" "H2 Database path"
                   :default "/dev/shm/rssminer"])
        (optional ["--index-path" "Path to store lucene index"
                   :default "/dev/shm/rssminer-index"]))))
