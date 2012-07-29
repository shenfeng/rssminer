(ns rssminer.fetcher-test
  (:use clojure.test
        rssminer.fetcher
        rssminer.db.feed
        rssminer.db.subscription
        [rssminer.util :only [now-seconds]]
        [rssminer.database :only [mysql-query mysql-insert]]
        [rssminer.test-common :only [app-fixture]])
  (:import me.shenfeng.http.HttpUtils))

(use-fixtures :each app-fixture
              (fn [test-fn]
                (mysql-insert :rss_links
                              {:url "http://aria42.com/blog/?feed=rss2"})
                (test-fn)))

(deftest test-mk-provider
  (let [provider ^rssminer.fetcher.IHttpTasksProvder (mk-provider)
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
    (is (= 1 (- (count (mysql-query ["select * from feeds"])) (count feeds))))
    (is (= 1 (- (count links) (count (fetch-rss-links 1000)))))))

(defn- insert-rss-link [link]
  (mysql-insert :rss_links link))

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
