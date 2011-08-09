(ns rssminer.util-test
  (:use rssminer.util
        clojure.data.json
        clojure.test)
  (:import java.util.Date))

(deftest test-md5-sum
  (is (= "e10adc3949ba59abbe56e057f20f883e" (md5-sum "123456"))))

(deftest test-write-json
  (is (json-str {:date (Date.)})))

(deftest test-assoc-if
  (is (= 3 (-> (assoc-if {} :a 1 :b 2 :c 3) keys count)))
  (is (= 2 (-> (assoc-if {} :a 1 :b 2 :c nil) keys count))))

