(ns rssminer.database
  (:use [clojure.java.jdbc :only [with-connection do-commands]]
        [clojure.tools.logging :only [debug info]]
        [clojure.java.jdbc :only [with-connection do-commands]])
  (:require [clojure.string :as str])
  (:import org.apache.commons.dbcp.BasicDataSource))

(defonce mysql-db-factory (atom {}))

(defn use-mysql-database! [url]
  (reset! mysql-db-factory
          (let [ds (doto (BasicDataSource.)
                     (.setUrl url)
                     (.setUsername "root")
                     (.setPassword ""))]
            {:factory (fn [& args] (.getConnection ds))
             :ds ds})))

(defn do-mysql-commands [& commands]
  (with-connection @mysql-db-factory
    (apply do-commands commands)))

(defn close-global-mysql-factory! []
  (if-let [ds (:ds @mysql-db-factory)]
    (.close ^BasicDataSource ds)
    (reset! mysql-db-factory nil)))

(defn import-mysql-schema! []
  (apply do-mysql-commands (filter (complement str/blank?)
                                   (str/split (slurp "src/rssminer.sql")
                                              #"\s*----*\s*"))))

(defmacro with-mysql [& body]
  `(with-connection @mysql-db-factory
     ~@body))

