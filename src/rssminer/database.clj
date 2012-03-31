(ns rssminer.database
  (:use [clojure.java.jdbc :only [with-connection do-commands]]
        [clojure.tools.logging :only [debug info]]
        [clojure.java.jdbc :only [with-connection do-commands]])
  (:require [clojure.string :as str])
  (:import java.sql.DriverManager))

(defonce mysql-db-factory (atom {}))

(defn use-mysql-database! [url]
  (reset! mysql-db-factory
          {:factory (fn [& args]
                      (DriverManager/getConnection url "root" ""))}))

(defn do-mysql-commands [& commands]
  (with-connection @mysql-db-factory
    (apply do-commands commands)))

(defn close-global-mysql-factory! []
  (reset! mysql-db-factory nil))

(defn import-mysql-schema! []
  (apply do-mysql-commands (filter (complement str/blank?)
                                   (str/split (slurp "src/rssminer.sql")
                                              #"\s*----*\s*"))))

(defmacro with-mysql [& body]
  `(with-connection @mysql-db-factory
     ~@body))

