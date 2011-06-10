(ns freader.test-util
  (:require [clojure.string :as str]
            [freader.config :as conf])
  (:use [freader.database :only [close-global-psql-factory
                                  use-psql-database!]]
        [freader.db.user :only [create-user]]
        [freader.test-common :only [test-user]]))

(defn- gen-random [num]
  (let [alphabet "abcdefghjklmnpqrstuvwxy123456789"]
    (apply str
           (take num (shuffle (seq alphabet))))))

(defn get-con [db]
  (java.sql.DriverManager/getConnection
   (str "jdbc:postgresql://" conf/DB_HOST "/" db)
   conf/PSQL_USERNAME conf/PSQL_PASSWORD))

(defn exec-stats [con & statements]
  (with-open [stmt (.createStatement con)]
    (doseq [s statements]
      (.addBatch stmt s))
    (.executeBatch stmt)))

(defn exec-prepared-sqlfile [tmpdb]
  (let [sql (slurp (-> (clojure.lang.RT/baseLoader)
                       (.getResourceAsStream "feedreader.sql")))
        stats (filter (complement str/blank?)
                      ;; use ----(4) to seperate sql statement
                      (str/split sql #"\s*----*\s*"))]
    (with-open [con (get-con tmpdb)]
      (apply exec-stats con stats))))

(defn postgresql-fixture [test-fn]
  (let [tmpdb (str "test_"  (.toLowerCase (gen-random 3)))]
    (with-open [con (get-con "postgres")]
      (exec-stats con (str "create database " tmpdb))
      (exec-prepared-sqlfile tmpdb)
      (use-psql-database! tmpdb)
      (create-user test-user)
      (test-fn)
      (close-global-psql-factory)       ; close global psql connetion
      (exec-stats con (str "drop database " tmpdb)))))
