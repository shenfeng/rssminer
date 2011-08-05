(ns freader.crawler-test
  (:use clojure.test
        (freader [test-common :only [h2-fixture tracep mocking]])
        freader.crawler
        [clojure.java.io :only [resource]])
  (:require [freader.db.crawler :as db]
            [freader.http :as http]))

(def headers {:last_modified "Sat, 23 Jul 2011 01:40:16 GMT"
              :server "Apache"})

(defn- mock-http-get [& args]
  {:status 200
   :headers headers
   :body (resource "page.html")})

(use-fixtures :each h2-fixture
              (fn [f] (binding [http/get mock-http-get]
                       (f))))

(deftest test-crawl-link
  (let [link (first (db/fetch-crawler-links 1))
        rss (db/fetch-rss-links 1000)
        links (db/fetch-crawler-links 1000)]
    (crawl-link link)
    (is (> (count (db/fetch-crawler-links 1000)) (count links)))
    (is (= 1 (- (count (db/fetch-rss-links 1000)) (count rss))))))

(deftest test-get-next-link
  (is (get-next-link)))

(deftest test-start-crawler
  (let [rss (db/fetch-rss-links 1000)]
    (mocking (var http/get) mock-http-get
             (let [crawler  (start-crawler)]
               (crawler :wait)))
    (is (nil? (db/fetch-crawler-links 2000)))
    (is (= 1 (- (count (db/fetch-rss-links 1000)) (count rss))))))
