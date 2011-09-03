(ns rssminer.database
  (:use [clojure.java.jdbc :only [with-connection do-commands]]
        [clojure.java.io :only [resource]]
        [clojure.tools.logging :only [debug info]]
        [clojure.java.jdbc :only [with-connection do-commands]])
  (:require [clojure.string :as str])
  (:import org.h2.jdbcx.JdbcConnectionPool
           org.h2.tools.Server))

(defonce h2-db-factory  (atom {:factory nil
                               :ds nil}))

(defn close-global-h2-factory! []
  (if-let [ds (:ds @h2-db-factory)]
    (.dispose ^JdbcConnectionPool ds)
    (reset! h2-db-factory nil)))

(defn use-h2-database! [db-path & {:keys [trace auto-server]}]
  (close-global-h2-factory!)
  (let [url (str "jdbc:h2:" db-path ";MVCC=true"
                 (when trace ";TRACE_LEVEL_FILE=2;TRACE_MAX_FILE_SIZE=1000")
                 (when auto-server ";AUTO_SERVER=TRUE"))
        ds (JdbcConnectionPool/create url "sa" "sa")
        f (fn [& args]  (.getConnection ds))]
    (debug "use h2 database" url)
    (reset! h2-db-factory {:factory f
                           :ds ds})))

(defn import-h2-schema! []
  (let [stats (filter (complement str/blank?)
                      (str/split (slurp (resource "rssminer.sql"))
                                 #"\s*----*\s*"))]
    (with-connection @h2-db-factory
      (apply do-commands (cons "DROP ALL OBJECTS" stats)))))
