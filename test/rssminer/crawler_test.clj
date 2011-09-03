(ns rssminer.crawler-test
  (:use clojure.test
        (rssminer [test-common :only [h2-fixture mocking]])
        [rssminer.db.util :only [h2-query]]
        rssminer.crawler
        [clojure.java.io :only [resource]])
  (:require [rssminer.db.crawler :as db]
            [rssminer.http :as http]))

(defn- mock-http-get [& args]
  {:status 200
   :headers {:last-modified "Sat, 23 Jul 2011 01:40:16 GMT"}
   :body (resource "page.html")})

(use-fixtures :each h2-fixture
              (fn [f] (binding [http/get mock-http-get]
                       (f))))

(deftest test-crawl-link
  (let [link (first (db/fetch-crawler-links 1))
        rss (db/fetch-rss-links 1000)
        links (db/fetch-crawler-links 1000)]
    (crawl-link link)
    (is (> (count (h2-query ["select * from crawler_links"])) (count links)))
    (is (= 1 (- (count (db/fetch-rss-links 1000)) (count rss))))))

(deftest test-get-next-link
  (is (get-next-link)))

(deftest test-start-crawler
  (let [rss (db/fetch-rss-links 1000)
        links (h2-query ["select * from crawler_links"])]
    (mocking (var http/get) mock-http-get
             (let [crawler (start-crawler :threads 1)]
               (crawler :wait)))
    (is (nil? (db/fetch-crawler-links 2000)))
    (testing "properly added and updated"
      (let [now (h2-query ["select * from crawler_links"])]
        (is (every? #(and (:last_modified %)
                          (:title %)) now))
        (is (> (count now) (count links)))))
    (is (= 1 (- (count (db/fetch-rss-links 1000)) (count rss))))))
