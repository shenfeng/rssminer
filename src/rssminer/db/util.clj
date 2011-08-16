(ns rssminer.db.util
  (:use [rssminer.database :only [h2-db-factory]]
        [clojure.walk :only [postwalk]]
        [clojure.java.jdbc :only [with-connection with-query-results
                                  insert-record as-identifier]])
  (:require [clojure.string :as str])
  (:import java.text.SimpleDateFormat
           java.util.Locale
           java.sql.Clob
           [java.sql Timestamp]))

(def id-k (keyword "scope_identity()"))

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

(defmacro with-h2 [& body]
  `(with-connection @h2-db-factory
     ~@body))

(defn h2-query [query]
  (with-h2
    (with-query-results rs query
      (postwalk (fn [x] (if (instance? Clob x)
                         (slurp (.getCharacterStream ^Clob x))
                         x))
                rs))))

(defn h2-insert-and-return [table record]
  (let [id (id-k (with-h2
                   (insert-record table record)))]
    (first (h2-query (select-sql-params table {:id id})))))

(defn parse-timestamp [str]
  (let [f (SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss ZZZ" Locale/US)]
    (Timestamp. (.getTime (.parse f str)))))
