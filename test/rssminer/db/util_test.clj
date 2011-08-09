(ns rssminer.db.util-test
  (:use clojure.test
        [rssminer.db.util :only [select-sql-params]])
  (:require [clojure.string :as str]))

(deftest test-select-sql-params
  (let [pred-map {:a 1 :b 2}
        [sql & params] (select-sql-params :table1 pred-map 10 0)
        expect-sql "SELECT * FROM table1 WHERE a = ? AND b = ? LIMIT ? OFFSET ?"
        expect-params (list 1 2 10 0)]
    (is (= sql expect-sql))
    (is (= params expect-params))))
