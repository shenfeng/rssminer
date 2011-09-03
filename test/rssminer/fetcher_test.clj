(ns rssminer.fetcher-test
  (:use clojure.test
        rssminer.fetcher
        [rssminer.db.util :only [h2-query]]
        [rssminer.test-common :only [h2-fixture mocking]]
        [clojure.java.io :only [resource]])
  (:require [rssminer.db.crawler :as db]
            [rssminer.http :as http]))

(defn- mock-http-get [& args]
  {:status 200
   :headers {:last-modified "Sat, 23 Jul 2011 01:40:16 GMT"}
   :body (resource "scottgu-atom.xml")})

(use-fixtures :each h2-fixture
              (fn [f] (binding [http/get mock-http-get]
                       (doseq [a (range 1 4)]
                         (db/insert-rss-link {:url (str "http://a.com/" a)}))
                       (f))))

(deftest test-fetch-rss
  (let [c (count (db/fetch-rss-links 1000))]
    (fetch-rss (get-next-link))
    (is (= 1 (- c (count (db/fetch-rss-links 1000)))))
    (is (= (count (h2-query ["select * from feeds"])) 1))))

(deftest test-fecher
  (mocking (var http/get) mock-http-get
           (let [fetcher (start-fetcher :threads 2)]
             (fetcher :wait)))
  (testing "everybody get the meta"
    (is (every? #(and (:alternate %)
                      (:title %)
                      (:last_modified %))
                (h2-query ["select * from rss_links"]))))
  (is (= (count (h2-query ["select * from feeds"])) 1)))


