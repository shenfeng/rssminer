(ns rssminer.http-test
  (:refer-clojure :exclude [get])
  (:use rssminer.http
        clojure.test
        (rssminer [test-common :only [h2-fixture]]
                  [config :only [add-black-domain-patten]]))
  (:import [org.jboss.netty.handler.codec.http DefaultHttpResponse
            HttpResponse HttpVersion HttpResponseStatus]
           org.jboss.netty.buffer.ChannelBuffers))

(use-fixtures :each h2-fixture)

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
         (resolve-url "http://a.com" "rss.html")))
  (is (= "http://a.com/c.html"
         (resolve-url "http://a.com/b.html" "c.html")))
  (is (nil? (resolve-url "http://a.com" " ")))
  (is (nil? (resolve-url "http://a.com" nil)))
  (is (= "http://a.com/a/c.html"
         (resolve-url "http://a.com/a/b/" "../c.html")))
  (is (= "http://c.com/c.html"
         (resolve-url "http://a.com" "http://c.com/c.html"))))

(deftest test-extract-links
  (let [html (slurp "test/page.html")
        {:keys [rss links title]} (extract-links "http://a.com/" html)]
    (is (> (count links) 0))
    (is (= title "Peter Norvig"))
    (is (every? #(and (:url %)
                      (:domain %)) links))
    (are [k] (-> rss first k)
         :title
         :url)
    (is (= 1 (count rss)))))

(deftest test-clean-url
  (is (= "http://a.com/c.php"
         (clean-url "http://a.com/c.php?t=blog&k=%C1%F4%D1%A7")))
  (is (nil? (clean-url "http://alohaitsluisa.tumblr.com/rss")))
  (add-black-domain-patten "a\\.com")
  (is (nil? (clean-url "http://a.com/c.php?t=blog&k=%C1%F4%D1%A7")))
  (is (nil? (clean-url "http://img.com/a.png")))
  (is (nil? (clean-url "http://img.com/a.JS")))
  (is (nil? (clean-url "http://jidikuabaoxiao06208392.founders-lawyer.com")))
  (is (nil? (clean-url "http://guangzhoufuzhuangsheyin04106333.sh-kbt.com"))))

(deftest test-parse-responce
  (let [resp (doto (DefaultHttpResponse. HttpVersion/HTTP_1_1
                     HttpResponseStatus/OK)
               (.setHeader "H1" "v1")
               (.setHeader "H2" "v2")
               (.setContent
                (ChannelBuffers/copiedBuffer (.getBytes "test body"))))
        r (parse-response resp)]
    (is (= 200 (:status r)))
    (is (= "v1" (-> r :headers :h1)))
    (is (= "v2" (-> r :headers :h2)))
    (is (= "test body" (:body r)))))
