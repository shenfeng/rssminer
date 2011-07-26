(ns freader.db.util
  (:use [freader.database :only [db-factory]]
        [clojure.java.io :only [resource]]
        [clojure.java.jdbc :only [with-connection with-query-results]])
  (:require [clojure.string :as str]
            [freader.config :as conf]))

(defn- escape-keyword [k]
  (str \" (name k) \"))

(defn select-sql-params
  ([table pred-map] (select-sql-params table pred-map 1 0))
  ([table pred-map limit offset]
     (let [pred-seg (str/join " AND "
                              (map #(str (escape-keyword %) " = ?")
                                   (keys pred-map)))
           values (concat (vals pred-map) (list limit offset))
           sql (list "SELECT * FROM "
                     (name table)
                     " WHERE "
                     pred-seg
                     " LIMIT ? OFFSET ?")]
       (apply vector (cons (apply str sql) values)))))

(defn insert-sql-params [table data-map]
  "concat insert sql, see test for more info"
  (let [values (vals data-map)
        place-holders (str/join ", " (repeat (count values) "?"))
        table-colums (str/join ", " (map escape-keyword
                                         (keys data-map)))
        sql (list "INSERT INTO "
                  (name table)
                  " ( "
                  table-colums
                  " ) VALUES ( "
                  place-holders
                  " ) RETURNING *")]
    (apply vector (cons (apply str sql) values))))

(defn update-sql-params
  ([table data-map] (update-sql-params table :id data-map))
  ([table pk data-map]
     (let [without-pk (dissoc data-map pk)
           update-segs (str/join ", "
                                 (map #(str (escape-keyword %) " = ?")
                                      (keys without-pk)))
           values (concat (vals without-pk) (list (pk data-map)))
           sql (list "UPDATE "
                     (name table)
                     " SET "
                     update-segs
                     " WHERE "
                     (escape-keyword pk)
                     " = ? RETURNING *")]
       (apply vector (cons (apply str sql) values)))))

(defn exec-query [sql-parms]
  (with-connection @db-factory
    (with-query-results rs sql-parms
      (doall rs))))

(defn get-con [db]
  (java.sql.DriverManager/getConnection
   (str "jdbc:postgresql://" conf/DB_HOST "/" db)
   conf/PSQL_USERNAME conf/PSQL_PASSWORD))

(defn exec-stats [con & statements]
  (with-open [stmt (.createStatement con)]
    (doseq [s statements]
      (.addBatch stmt s))
    (.executeBatch stmt)))

(defn exec-prepared-sqlfile [db-name]
  (let [stats (filter
               (complement str/blank?)
               (str/split ;; use ----(4) to seperate sql statement
                (slurp (resource "freader.sql")) #"\s*----*\s*"))]
    (with-open [con (get-con db-name)]
      (apply exec-stats con stats))))
