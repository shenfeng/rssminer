(ns freader.util-test
  (:use freader.util
        clojure.data.json
        clojure.test)
  (:import java.util.Date))

(deftest test-md5-sum
  (is (= "e10adc3949ba59abbe56e057f20f883e" (md5-sum "123456"))))

(deftest test-write-json
  (is (json-str {:date (Date.)})))
