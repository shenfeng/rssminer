(ns rssminer.database
  (:use [clojure.java.jdbc :only [with-connection do-commands]]
        [clojure.java.io :only [resource]]
        [clojure.java.jdbc :only [with-connection do-commands]])
  (:require [clojure.string :as str])
  (:import org.h2.jdbcx.JdbcConnectionPool))

(defonce h2-db-factory  (atom {:factory nil
                               :ds nil}))

(defn close-global-h2-factory! []
  (if-let [ds (:ds @h2-db-factory)]
    (.dispose ds)
    (reset! h2-db-factory nil)))

(defn use-h2-database! [file & {:keys [trace]}]
  (close-global-h2-factory!)
  (let [url (str "jdbc:h2:" file
                 (when trace ";TRACE_LEVEL_FILE=2;TRACE_MAX_FILE_SIZE=1000"))
        ds (JdbcConnectionPool/create url "sa" "sa")
        f (fn [& args]  (.getConnection ds))]
    (reset! h2-db-factory {:factory f
                           :ds ds})))

(defn import-h2-schema! []
  (let [stats (filter (complement str/blank?)
                      (str/split (slurp (resource "feed_crawler.sql"))
                                 #"\s*----*\s*"))]
    (with-connection @h2-db-factory
      (apply do-commands (cons "DROP ALL OBJECTS" stats)))))
