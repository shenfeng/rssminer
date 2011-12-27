(ns rssminer.crawler-test
  (:use clojure.test
        (rssminer [test-common :only [h2-fixture]]
                  [http :only [extract-links]])
        [rssminer.db.util :only [h2-query]]
        rssminer.crawler)
  (:require [rssminer.db.crawler :as db]))

(use-fixtures :each h2-fixture)

(deftest test-mk-provider
  (let [provider ^rssminer.task.IHttpTasksProvder (mk-provider)
        task (first (.getTasks provider))]
    (is (.getUri task))
    (is (empty? (.getHeaders task)))))

(deftest test-handle-resp
  (let [links (db/fetch-crawler-links 10000)]
    (handle-resp (first links)
                 {:status 200
                  :headers {:last-modified "Sat, 23 Jul 2011 01:40:16 GMT"}
                  :body (slurp "test/page.html")})
    (is (> 0 (- (count links) (count (db/fetch-crawler-links 1000)))))))

(deftest test-extract-and-save-links
  (let [info (extract-links "http://me.me" (slurp "test/page.html"))
        rss (h2-query ["select * from rss_links"])
        links (h2-query ["select * from crawler_links"])]
    (save-links {:id 1 :url "http://me.me"}
                (:links info) (:rss info))
    (is (> (count (h2-query ["select * from rss_links"])) (count rss)))
    (is (> (count (h2-query ["select * from crawler_links"]))
           (count links)))))
