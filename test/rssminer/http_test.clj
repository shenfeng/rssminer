(ns rssminer.http-test
  (:refer-clojure :exclude [get])
  (:use rssminer.http
        [clojure.java.io :only [resource]]
        clojure.test))

(deftest test-extract-host
  (is (= (extract-host "http://192.168.1.11:8000/#change,83")
         "http://192.168.1.11:8000"))
  (is (= (extract-host "https://github.com/shenfeng/onycloud/blob/master/books/src/trakr/routes.clj")
         "https://github.com")))

(deftest test-resolve-url
  (is (= "http://a.com/c.html"
         (resolve-url "http://a.com/index?a=b" "c.html")))
  (is (= "http://a.com/rss.html"
         (resolve-url "http://a.com" "rss.html")))
  (is (= "http://a.com/c.html"
         (resolve-url "http://a.com/b.html" "c.html")))
  (is (= "http://a.com/a/c.html"
         (resolve-url "http://a.com/a/b/" "../c.html")))
  (is (= "http://c.com/c.html"
         (resolve-url "http://a.com" "http://c.com/c.html"))))

(deftest test-extract-links
  (let [html (slurp (resource "page.html"))
        links (extract-links "http://a.com/" html)]
    (is (= 10 (-> links :links count)))
    (is (= 1 (-> links :rss count)))))
