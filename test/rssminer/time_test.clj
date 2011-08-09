(ns rssminer.time-test
  (:use rssminer.time
        clojure.test))

(deftest test-now
  (is (now))
  (is (now-seconds)))
