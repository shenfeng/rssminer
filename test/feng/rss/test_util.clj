(ns feng.rss.test-util
  (:require [clojure.string :as str])
  (:use [feng.rss.database :only [close-global-psql-factory
                                  use-psql-database!]]
        [feng.rss.db.user :only [create-user]]
        [feng.rss.test-common :only [test-user]]))

(def TEST_DB_HOST
  (get (System/getenv) "READER_DB_HOST" "127.0.0.1"))
(def TEST_PSQL_USERNAME "postgres")
(def TEST_PSQL_PASSWORD "123456")

(defn- gen-random [num]
  (let [alphabet "abcdefghjklmnpqrstuvwxy123456789"]
    (apply str
           (take num (shuffle (seq alphabet))))))

(defn exec-prepared-sqlfile [tmpdb]
  (let [sql (slurp (-> (clojure.lang.RT/baseLoader)
                       (.getResourceAsStream "feedreader.sql")))
        stats (filter (complement str/blank?)
                      ;; use ----(4) to seperate sql statement
                      (str/split sql #"\s*----*\s*"))
        con (java.sql.DriverManager/getConnection
             (str "jdbc:postgresql://" TEST_DB_HOST "/" tmpdb)
             TEST_PSQL_USERNAME TEST_PSQL_PASSWORD)
        executor (.createStatement con)]
    (doseq [s stats]
      (.addBatch executor s))
    (.executeBatch executor)
    (.close con)))

(defn postgresql-fixture [test-fn]
  (let [tmpdb (str "test_"  (.toLowerCase (gen-random 3)))
        create-sql (str "create database " tmpdb)
        drop-sql (str "drop database " tmpdb)
        con-uri (str "jdbc:postgresql://" TEST_DB_HOST "/postgres")
        con (java.sql.DriverManager/getConnection
             con-uri TEST_PSQL_USERNAME TEST_PSQL_PASSWORD)]
    (.close (doto (.createStatement con) ; create a temp postgres database
              (.addBatch create-sql)
              (.executeBatch)))
    (use-psql-database! :jdbc-url (str "jdbc:postgresql://"
                                       TEST_DB_HOST "/" tmpdb)
                        :user TEST_PSQL_USERNAME
                        :password TEST_PSQL_PASSWORD)
    (exec-prepared-sqlfile tmpdb)
    (create-user test-user)
    (test-fn)
    (close-global-psql-factory)         ; close global psql connetion
    (doto (.createStatement con)        ; drop test psql database
      (.addBatch drop-sql)
      (.executeBatch))
    (.close con)))
