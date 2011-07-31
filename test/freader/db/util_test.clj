(ns freader.db.util-test
  (:use clojure.test
        [freader.db.util :only [insert-sql-params update-sql-params
                                select-sql-params]])
  (:require [clojure.string :as str]))

(deftest test-select-sql-params
  (let [pred-map {:a 1 :b 2}
        [sql & params] (select-sql-params :table1 pred-map 10 0)
        expect-sql "SELECT * FROM table1 WHERE \"a\" = ? AND \"b\" = ? LIMIT ? OFFSET ?"
        expect-params (list 1 2 10 0)]
    (is (= sql expect-sql))
    (is (= params expect-params))))

(deftest test-insert-sql-params
  (let [m {:value 1 :id 2}
        [sql & params] (insert-sql-params :table1 m)
        expect-sql "INSERT INTO table1 ( \"value\", \"id\" ) VALUES ( ?, ? ) RETURNING *"
        expect-params (list 1 2)]
    (is (= sql expect-sql))
    (is (= params expect-params)))
  (let [m {:id 5 :value 2}
        [sql & params] (insert-sql-params :table2 m)
        expect-sql "INSERT INTO table2 ( \"id\", \"value\" ) VALUES ( ?, ? ) RETURNING *"
        expect-params (list 5 2)]
    (is (= sql expect-sql))
    (is (= params expect-params))))

(deftest test-update-sql-params
  (let [m {:id 11 :c1 2 :c2 3}
        [sql & params] (update-sql-params :table1 m)
        expect-sql "UPDATE table1 SET \"c1\" = ?, \"c2\" = ? WHERE \"id\" = ? RETURNING *"
        expect-params (list 2 3 11)]
    (is (= sql expect-sql))
    (is (= params expect-params)))
  (let [m {:id 10 :pk "aaa" :m 1}
        [sql & params] (update-sql-params :table2 :pk m)
        expect-sql "UPDATE table2 SET \"id\" = ?, \"m\" = ? WHERE \"pk\" = ? RETURNING *"
        expect-params (list 10 1 "aaa")]
    (is (= sql expect-sql))
    (is (= params expect-params))))
