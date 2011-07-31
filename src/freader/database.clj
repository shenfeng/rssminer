(ns freader.database
  (:require [freader.config :as conf])
  (:import (clojure.lang RT)
           org.h2.jdbcx.JdbcConnectionPool
           (org.apache.commons.dbcp BasicDataSource)))

(defonce db-factory  (atom {:factory nil
                            :ds nil}))

(defonce h2-db-factory  (atom {:factory nil
                               :ds nil}))

(defn close-global-psql-factory []
  (if-let [ds (:ds @db-factory)]
    (.close ds)
    (reset! db-factory nil)))

(defn use-psql-database!
  ([db-name]
     (use-psql-database! (str "jdbc:postgresql://" conf/DB_HOST "/" db-name)
                         conf/PSQL_USERNAME
                         conf/PSQL_PASSWORD))
  ([jdbc-url user password]
     (RT/loadClassForName "org.postgresql.Driver")
     (close-global-psql-factory)
     ;; TODO investigate other connection pool options, eg: BoneCP
     (let [ds (doto (BasicDataSource.)
                (.setUrl jdbc-url)
                (.setUsername user)
                (.setPassword password))
           f (fn [& args]  (.getConnection ds))]
       (reset! db-factory {:factory f
                           :ds ds}))))

(defn close-global-h2-factory []
  (if-let [ds (:ds @h2-db-factory)]
    (.dispose ds)
    (reset! h2-db-factory nil)))

(defn use-h2-database! [file]
  (close-global-h2-factory)
  (let [ds (JdbcConnectionPool/create (str "jdbc:h2:" file)
                                      "sa" "sa")
        f (fn [& args]  (.getConnection ds))]
    (reset! h2-db-factory {:factory f
                           :ds ds})))
