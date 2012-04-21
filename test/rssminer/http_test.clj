(ns rssminer.http-test
  (:refer-clojure :exclude [get])
  (:use rssminer.http
        clojure.test)
  (:import [org.jboss.netty.handler.codec.http DefaultHttpResponse
            HttpResponse HttpVersion HttpResponseStatus]
           org.jboss.netty.buffer.ChannelBuffers))

(deftest test-extract-host
  (is (= (extract-host "http://192.168.1.11:8000/#change,83")
         "http://192.168.1.11:8000"))
  (is (= (extract-host "https://github.com/master/books/src/trakr/routes.clj")
         "https://github.com")))

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
