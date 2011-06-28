(ns freader.test-util
  (:use (freader [database :only [close-global-psql-factory use-psql-database!]]
                 [test-common :only [test-user]]
                 [search :only [use-index-writer! close-global-index-writer!]])
        (freader.db [user :only [create-user]]
                    [util :only [get-con exec-stats exec-prepared-sqlfile]])))

(defn- gen-random [num]
  (let [alphabet "abcdefghjklmnpqrstuvwxy123456789"]
    (apply str
           (take num (shuffle (seq alphabet))))))

(defn postgresql-fixture [test-fn]
  (let [tmpdb (str "test_"  (.toLowerCase (gen-random 3)))]
    (with-open [con (get-con "postgres")]
      (try
        (exec-stats con (str "create database " tmpdb))
        (exec-prepared-sqlfile tmpdb)
        (use-psql-database! tmpdb)
        (create-user test-user)
        (test-fn)
        (finally (close-global-psql-factory) ; close global psql connetion
                 (exec-stats con (str "drop database " tmpdb)))))))

(defn lucene-fixture [test-fn]
  (use-index-writer! :RAM)
  (test-fn)
  (close-global-index-writer!))
