(ns rssminer.admin
  (:gen-class)
  (:use (rssminer [search :only [index-feed use-index-writer!
                                 close-global-index-writer!]]
                  [util :only [user-id-from-session to-int]]
                  [classify :only [on-feed-event]]
                  [database :only [mysql-query with-mysql mysql-insert]])
        (clojure.tools [logging :only [info]]
                       [cli :only [cli]])
        [clojure.java.jdbc :only [with-query-results]])
  (:require [rssminer.database :as db]))

(defn rebuild-index []
  (info "Clear all lucene index and rebuild it")
  (with-mysql
    (with-query-results rs ["select * from feeds"]
      (doseq [feed rs]
        (index-feed (:id feed) (:rss_link_id feed) feed))))
  (close-global-index-writer!)
  (info "Rebuild index OK"))

(defn recompute-scores [req]
  (let [uid (-> req :params :id)
        me (user-id-from-session req)]
    (if (and (= me 1) uid) ;; user-id 1 is myself, who has admin right
      (do (on-feed-event (to-int uid) (to-int -1))
          {:status 200 :body "ok"})
      {:status 401 :body "error"})))

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

