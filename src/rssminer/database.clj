(ns rssminer.database
  (:use [clojure.java.jdbc :only [with-connection do-commands]]
        [clojure.tools.logging :only [debug info]]
        [rssminer.config :only [rssminer-conf]]
        [clojure.java.jdbc :only [with-connection do-commands
                                  as-identifier
                                  with-query-results insert-record]])
  (:require [clojure.string :as str])
  (:import me.shenfeng.dbcp.PerThreadDataSource
           java.text.SimpleDateFormat
           java.util.Locale
           java.sql.Timestamp))

(defonce mysql-db-factory (atom {}))

(defn use-mysql-database! [url user]
  (let [ds (PerThreadDataSource. url user "")]
    (swap! rssminer-conf assoc :data-source ds)
    (reset! mysql-db-factory {:factory (fn [& args] (.getConnection ds))
                              :ds ds})))

(defn do-mysql-commands [& commands]
  (with-connection @mysql-db-factory
    (apply do-commands commands)))

(defn close-global-mysql-factory! []
  (if-let [ds (:ds @mysql-db-factory)]
    (.close ^PerThreadDataSource ds)
    (reset! mysql-db-factory nil)))

(defmacro with-mysql [& body]
  `(with-connection @mysql-db-factory
     ~@body))

(defn select-sql-params
  ([table pred-map] (select-sql-params table pred-map 1 0))
  ([table pred-map limit offset]
     (let [pred-seg (str/join " AND "
                              (map #(str (as-identifier %) " = ?")
                                   (keys pred-map)))
           values (concat (vals pred-map) (list limit offset))
           sql (list "SELECT * FROM "
                     (name table)
                     " WHERE "
                     pred-seg
                     " LIMIT ? OFFSET ?")]
       (apply vector (cons (apply str sql) values)))))

(defn mysql-query [query]
  (with-mysql (with-query-results rs query (doall rs))))

(defn mysql-insert [table record]
  (:generated_key (with-mysql (insert-record table record))))

(defn mysql-insert-and-return [table record]
  (let [id (mysql-insert table record)]
    (first (mysql-query (select-sql-params table {:id id})))))

(defn parse-timestamp [str]
  (let [f (SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss ZZZ" Locale/US)]
    (Timestamp. (.getTime (.parse f str)))))

