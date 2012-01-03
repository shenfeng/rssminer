(ns rssminer.fetcher-test
  (:use clojure.test
        rssminer.fetcher
        rssminer.db.feed
        [rssminer.time :only [now-seconds]]
        [rssminer.db.util :only [h2-query]]
        [rssminer.test-common :only [h2-fixture]]))

(use-fixtures :each h2-fixture)

(deftest test-mk-provider
  (let [provider ^rssminer.task.IHttpTasksProvder (mk-provider)
        task (first (.getTasks provider))]
    (is (.getUri task))
    (is (empty? (.getHeaders task)))))

(deftest test-handle-resp
  (let [links (fetch-rss-links 10000)
        feeds (h2-query ["select * from feeds"])]
    (handle-resp (first links)
                 {:status 200
                  :headers {:last-modified "Sat, 23 Jul 2011 01:40:16 GMT"}
                  :body (slurp "test/scottgu-atom.xml")})
    (is (= (count (h2-query ["select * from feeds"])) 1))
    (is (= 1 (- (count links) (count (fetch-rss-links 1000)))))))

(deftest test-insert-rss-link
  (let [links (fetch-rss-links 1000)
        newly (insert-rss-link {:url "http://a.com/feed.xml"})]
    (is newly)
    (is (= 1 (- (count (fetch-rss-links 100)) (count links))))))

(deftest test-update-rss-link
  (let [newly (insert-rss-link {:url "http://a.com/feed.xml"})
        c (count (fetch-rss-links 100))
        u (update-rss-link newly {:check_interval 100
                                  :next_check_ts (+ (now-seconds) 1000)})]
    (is (= 1 (- c (count (fetch-rss-links 100)))))))
