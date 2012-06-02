(ns rssminer.database
  (:use [clojure.java.jdbc :only [with-connection with-query-results
                                  insert-record as-identifier]])
  (:require [clojure.string :as str])
  (:import java.text.SimpleDateFormat
           java.util.Locale
           java.sql.Timestamp))

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

(defmacro with-mysql [& body]
  `(with-connection @mysql-db-factory
     ~@body))

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
