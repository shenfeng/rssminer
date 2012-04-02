(ns rssminer.admin
  (:use (rssminer [search :only [searcher index-feed use-index-writer!]]
                  [util :only [ignore-error to-int]])
        (rssminer.db [user :only [create-user]]
                     [util :only [mysql-query with-mysql mysql-insert]])
        (clojure.tools [logging :only [info]]
                       [cli :only [cli]])
        [clojure.java.jdbc :only [with-query-results]])
  (:require [clojure.string :as str]
            [rssminer.database :as db])
  (:import rssminer.Searcher))

(defn setup-db [{:keys [db-name email password]}]
  (db/use-mysql-database! (str "jdbc:mysql://localhost/mysql"))
  (db/do-mysql-commands (str "drop database if exists " db-name)
                        (str "create database if not exists " db-name))
  (db/use-mysql-database! (str "jdbc:mysql://localhost/" db-name))
  (info "Import mysql schema, Create first user" email)
  (create-user {:email email :password password})
  (db/close-global-mysql-factory!))

(defn rand-subscribe []
  (let [rrs (filter identity
                    (map #(first (mysql-query
                                  ["SELECT id, title FROM rss_links
                        WHERE title IS NOT NULL AND id > ? LIMIT 1" %]))
                         (set (repeatedly 45 #(rand-int 10000)))))]
    (doseq [r rrs]
      (ignore-error (mysql-insert :user_subscription
                                  {:user_id 1
                                   :rss_link_id (:id r)
                                   :title (:title r)})))))

(defn rebuild-index []
  (.toggleInfoStream ^Searcher @searcher true)
  (.clear ^Searcher @searcher)
  (with-mysql
    (with-query-results rs ["select * from feeds"]
      (doseq [feed rs]
        (index-feed (:id feed) feed))))
  (.toggleInfoStream ^Searcher @searcher false))

(defn -main [& args]
  "Rssminer admin"
  (let [[options _ banner]
        (cli args
             ["-c" "--command"
              "init-db, rebuild-index"
              :parse-fn keyword]
             ["-p" "--password" "First user's password" :default "123456"]
             ["-e" "--email" "First user's username"
              :default "shenedu@gmail.com"]
             ["--db-name" "Mysql Database name" :default "rssminer"]
             ["--index-path" "Path to store lucene index"
              :default "/var/rssminer/index"]
             ["--[no-]help" "Print this help"])]
    (when (:help options) (println banner) (System/exit 0))
    (case (:command options)
      :init-db (setup-db options)
      :rebuild-index (do (db/use-mysql-database! (:db-name options))
                         (use-index-writer! (:index-path options))
                         (rebuild-index)))))

