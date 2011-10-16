(ns rssminer.database
  (:use [clojure.java.jdbc :only [with-connection do-commands]]
        [clojure.tools.logging :only [debug info]]
        [clojure.java.jdbc :only [with-connection do-commands]])
  (:require [clojure.string :as str])
  (:import org.h2.jdbcx.JdbcConnectionPool
           org.h2.tools.Server))

(defonce h2-db-factory  (atom {:factory nil
                               :ds nil}))

(defonce server (atom nil))

(defn running? []
  (not (nil? @server)))

(defn start-h2-server []
  (when-not (running?)
    (reset! server (doto (Server.)
                     (.runTool (into-array '("-tcp" "-tcpAllowOthers")))))))

(defn stop-h2-server []
  (when (running?)
    (.shutdown ^Server @server)
    (reset! server nil)))

(defn close-global-h2-factory! []
  (if-let [ds (:ds @h2-db-factory)]
    (.dispose ^JdbcConnectionPool ds)
    (reset! h2-db-factory nil)))

(defn use-h2-database! [db-path & {:keys [trace auto-server]}]
  (close-global-h2-factory!)
  (let [url (str "jdbc:h2:" db-path ";MVCC=true"
                 (when trace ";TRACE_LEVEL_FILE=2;TRACE_MAX_FILE_SIZE=1000")
                 (when auto-server ";AUTO_SERVER=TRUE"))
        ds (JdbcConnectionPool/create url "sa" "")
        f (fn [& args]  (.getConnection ds))]
    (info "use h2 database" url)
    (reset! h2-db-factory {:factory f
                           :ds ds})))

(defn import-h2-schema! []
  (let [stats (filter (complement str/blank?)
                      (str/split (slurp "src/rssminer.sql")
                                 #"\s*----*\s*"))]
    (with-connection @h2-db-factory
      (apply do-commands (cons "DROP ALL OBJECTS" stats)))))
