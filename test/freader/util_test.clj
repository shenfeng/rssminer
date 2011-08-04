(ns freader.util-test
  (:use freader.util
        clojure.data.json
        clojure.pprint
        clojure.test
        [clojure.java.io :only [resource]])
  (:import java.util.Date))

(deftest test-md5-sum
  (is (= "e10adc3949ba59abbe56e057f20f883e" (md5-sum "123456"))))

(deftest test-write-json
  (is (json-str {:date (Date.)})))

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
