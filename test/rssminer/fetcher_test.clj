(ns rssminer.fetcher-test
  (:use clojure.test
        rssminer.fetcher
        [rssminer.db.util :only [h2-query]]
        [rssminer.test-common :only [h2-fixture]])
  (:require [rssminer.db.crawler :as db]))

(use-fixtures :each h2-fixture)

(deftest test-mk-provider
  (let [provider ^rssminer.task.IHttpTaskProvder (mk-provider)
        task (first (.getTasks provider))]
    (is (.getUri task))
    (is (empty? (.getHeaders task)))))

(deftest test-handle-resp
  (let [links (db/fetch-rss-links 10000)
        feeds (h2-query ["select * from feeds"])]
    (handle-resp (first links)
                 {:status 200
                  :headers {:last-modified "Sat, 23 Jul 2011 01:40:16 GMT"}
                  :body (slurp "test/scottgu-atom.xml")})
    (is (= (count (h2-query ["select * from feeds"])) 1))
    (is (= 1 (- (count links) (count (db/fetch-rss-links 1000)))))))
