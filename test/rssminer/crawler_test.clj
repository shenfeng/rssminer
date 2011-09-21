(ns rssminer.crawler-test
  (:use clojure.test
        (rssminer [test-common :only [h2-fixture mocking]])
        [rssminer.db.util :only [h2-query]]
        rssminer.crawler)
  (:require [rssminer.db.crawler :as db]
            [rssminer.http :as http]))

(use-fixtures :each h2-fixture)

(deftest test-mk-provider
  (let [provider ^rssminer.task.IHttpTaskProvder (mk-provider)
        task (.nextTask provider)]
    (is (.getUri task))
    (is (empty? (.getHeaders task)))))

(deftest test-get-next-link
  (is (get-next-link (java.util.LinkedList.))))

(deftest test-extract-and-save-links
  (let [info (http/extract-links "http://me.me" (slurp "test/page.html"))
        rss (h2-query ["select * from rss_links"])
        links (h2-query ["select * from crawler_links"])]
    (extract-and-save-links {:id 1 :url "http://me.me"}
                            (:links info) (:rss info))
    (is (> (count (h2-query ["select * from rss_links"])) (count rss)))
    (is (> (count (h2-query ["select * from crawler_links"]))
           (count links)))))
