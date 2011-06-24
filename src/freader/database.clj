(ns freader.database
  (:require [freader.config :as conf])
  (:import (clojure.lang RT)
           (org.apache.commons.dbcp BasicDataSource)))

(def ^{:dynamic true} *factory*  {:factory nil
                                 :ds nil})

(defn close-global-psql-factory []
  (if-let [ds (:ds *factory*)]
    (.close ds)))

(defn use-psql-database!
  ([db-name]
     (use-psql-database! (str "jdbc:postgresql://" conf/DB_HOST "/" db-name)
                         conf/PSQL_USERNAME
                         conf/PSQL_PASSWORD))
  ([jdbc-url user password]
     (RT/loadClassForName "org.postgresql.Driver")
     ;; TODO investigate other connection pool options, eg: BoneCP
     (let [ds (doto (BasicDataSource.)
                (.setUrl jdbc-url)
                (.setUsername user)
                (.setPassword password))
           f (fn [& args]  (.getConnection ds))]
       (close-global-psql-factory)
       (def *factory* {:factory f
                       :ds ds}))))
