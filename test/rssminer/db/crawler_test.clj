(ns rssminer.db.crawler-test
  (:use clojure.test
        rssminer.db.crawler
        [rssminer.db.util :only [id-k]]
        (rssminer [test-common :only [h2-fixture]]
                  [time :only [now-seconds]])))

(use-fixtures :each h2-fixture)

(deftest test-fetch-crawler-links
  (is (= 5 (count (fetch-crawler-links 5)))))

(deftest test-insert-crawler-links
  (let [saved (fetch-crawler-links 5)
        refer-id (-> saved first :id)
        newly [{:url "http://a.com/a" :domain "http://a.com"}
               {:url "http://b.com/b" :domain "http://b.com"}
               {:url "http://a.com/ab" :domain "http://a.com"}]]
    (is (= 0 (count (insert-crawler-links
                     (map #(assoc {}
                             :url (:url %)
                             :referer_id refer-id
                             :next_check_ts (rand-int 1000000)
                             :title "sample a text") saved)))))
    (is (= 2 (count (insert-crawler-links
                     (map #(assoc % :referer_id refer-id
                                  :next_check_ts (rand-int 1000)) newly)))))))

(deftest test-update-crawler-link
  (let [links (fetch-crawler-links 3)
        updates (doall (map #(update-crawler-link
                              (:id %) {:next_check_ts 1}) links))]
    (is (every? #(= 1 %) (flatten updates)))))

(deftest test-insert-rss-link
  (let [links (fetch-rss-links 1000)
        newly (insert-rss-link {:url "http://a.com/feed.xml"})]
    (is (id-k newly))
    (is (= 1 (- (count (fetch-rss-links 100)) (count links))))))

(deftest test-update-rss-link
  (let [newly (insert-rss-link {:url "http://a.com/feed.xml"})
        u (update-rss-link (id-k newly)
                           {:check_interval 100
                            :next_check_ts (- (now-seconds) 1000)})]
    (is (= 1 (count (fetch-rss-links 100))))))
