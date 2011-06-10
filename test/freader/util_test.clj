(ns freader.util-test
  (:use freader.util
        clojure.test))

(deftest test-get-host
  (is (= (get-host "http://192.168.1.11:8000/#change,83") "http://192.168.1.11:8000"))
  (is (= (get-host "https://github.com/shenfeng/onycloud/blob/master/books/src/trakr/routes.clj")
         "https://github.com")))

