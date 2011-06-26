(ns freader.http-test
  (:refer-clojure :exclude [get])
  (:use freader.http
        clojure.test))

(deftest test-extract-host
  (is (= (extract-host "http://192.168.1.11:8000/#change,83")
         "http://192.168.1.11:8000"))
  (is (= (extract-host "https://github.com/shenfeng/onycloud/blob/master/books/src/trakr/routes.clj")
         "https://github.com")))

