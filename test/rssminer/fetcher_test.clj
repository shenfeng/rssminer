(ns rssminer.fetcher-test
  (:use clojure.test
        rssminer.fetcher
        rssminer.db.feed
        [rssminer.util :only [now-seconds]]
        [rssminer.db.util :only [mysql-query mysql-insert]]
        [rssminer.test-common :only [mysql-fixture]])
  (:import me.shenfeng.http.HttpUtils))

(use-fixtures :each mysql-fixture
              (fn [test-fn]
                (mysql-insert :rss_links
                              {:url "http://aria42.com/blog/?feed=rss2"})
                (test-fn)))

(deftest test-mk-provider
  (let [provider ^rssminer.task.IHttpTasksProvder (mk-provider)
        task (first (.getTasks provider))]
    (is (.getUri task))
    (let [header (.getHeaders task)]
      (is (nil? (get header HttpUtils/IF_MODIFIED_SINCE {}))
          (nil? (get header HttpUtils/IF_NONE_MATCH {}))))))

(deftest test-handle-resp
  (let [links (fetch-rss-links 10000)
        feeds (mysql-query ["select * from feeds"])]
    (handle-resp (first links)
                 200
                 {"Last-Modified" "Sat, 23 Jul 2011 01:40:16 GMT"}
                 (slurp "test/scottgu-atom.xml"))
    (is (= (count (mysql-query ["select * from feeds"])) 1))
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
