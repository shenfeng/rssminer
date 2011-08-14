(ns rssminer.http-test
  (:refer-clojure :exclude [get])
  (:use rssminer.http
        [clojure.java.io :only [resource]]
        clojure.test))

(deftest test-extract-host
  (is (= (extract-host "http://192.168.1.11:8000/#change,83")
         "http://192.168.1.11:8000"))
  (is (= (extract-host "https://github.com/master/books/src/trakr/routes.clj")
         "https://github.com")))

(deftest test-resolve-url
  (is (= "http://a.com/c.html"
         (resolve-url "http://a.com/index?a=b" "c.html")))
  (is (= "http://a.com/c.html?a=b"
         (resolve-url "http://a.com/index?a=b" "c.html?a=b")))
  (is (= "http://a.com/rss.html"
         (resolve-url "http://a.com" "rss.html ")))
  (is (= "http://a.com/c.html"
         (resolve-url "http://a.com/b.html" "c.html")))
  (is (nil? (resolve-url "http://a.com" "Javascript:open()")))
  (is (nil? (resolve-url "http://a.com" "mailTo:sb@a.com")))
  (is (nil? (resolve-url "http://a.com" "#111")))
  (is (nil? (resolve-url "http://a.com" " #111")))
  (is (nil? (resolve-url "http://a.com" " ")))
  (is (nil? (resolve-url "http://a.com" nil)))
  (is (= "http://a.com/a/c.html"
         (resolve-url "http://a.com/a/b/" "../c.html")))
  (is (= "http://c.com/c.html"
         (resolve-url "http://a.com" "http://c.com/c.html"))))

(deftest test-extract-links
  (let [html (slurp (resource "page.html"))
        {:keys [rss links]} (extract-links "http://a.com/" html)]
    (is (> (count links) 0))
    (is (every? #(and (:url %)
                      (:domain %)) links))
    (are [k] (-> rss first k)
         :title
         :url)
    (is (= 1 (count rss)))))

(deftest test-clean-url
  (is (= "http://a.cn/c.php"
       (clean-url "http://a.cn/c.php?t=blog&k=%C1%F4%D1%A7"))))

