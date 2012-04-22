(ns rssminer.admin
  (:use (rssminer [search :only [index-feed use-index-writer!
                                 close-global-index-writer!]])
        (rssminer.db [util :only [mysql-query with-mysql mysql-insert]])
        (clojure.tools [logging :only [info]]
                       [cli :only [cli]])
        [clojure.java.jdbc :only [with-query-results]])
  (:require [rssminer.database :as db]))

(defn rebuild-index []
  (info "Clear all lucene index and rebuild it")
  (with-mysql
    (with-query-results rs ["select * from feeds"]
      (doseq [feed rs]
        (index-feed (:id feed) feed))))
  (close-global-index-writer!)
  (info "Rebuild index OK"))

(defn -main [& args]
  "Rssminer admin"
  (let [[options _ banner]
        (cli args
             ["-c" "--command" "rebuild-index" :parse-fn keyword]
             ["--db-url" "Mysql Database url"
              :default "jdbc:mysql://localhost/rssminer"]
             ["--db-user" "Mysql Database user name" :default "feng"]
             ["--index-path" "Path to store lucene index"
              :default "/var/rssminer/index"]
             ["--[no-]help" "Print this help"])]
    (when (:help options) (println banner) (System/exit 0))
    (if (= (:command options) :rebuild-index)
      (do (db/use-mysql-database! (:db-url options)
                                  (:db-user options))
          (use-index-writer! (:index-path options))
          (rebuild-index)))))

