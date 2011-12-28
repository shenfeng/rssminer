(ns rssminer.db.feed-test
  (:use clojure.test
        rssminer.db.feed
        [rssminer.db.util :only [h2-query with-h2 h2-insert-and-return]]
        (rssminer [test-common :only [user1 app-fixture mk-feeds-fixtrue]])))

(use-fixtures :each app-fixture (mk-feeds-fixtrue "test/scottgu-atom.xml"))

(deftest test-fetch-for-nav
  (let [unread (fetch-unread-meta (:id user1))]
    (is (= 1 (count unread)))))

(deftest test-update-rss-link
  (let [r1 (h2-insert-and-return :rss_links {:url "http://link1.com"})
        r2 (h2-insert-and-return :rss_links {:url "http://link2.com"})
        us (h2-insert-and-return :user_subscription {:user_id (:id user1)
                                                     :rss_link_id (:id r1)})]
    (update-rss-link (:id r1) {:url "http://link2.com"})
    (is (not (-> (h2-query ["SELECT id FROM rss_links WHERE url = ?"
                            "http://link1.com"]) first :id)))
    (is (= (:id r2) (-> (h2-query ["SELECT id from rss_links WHERE url = ?"
                                   "http://link2.com"]) first :id)))
    (is (= (:id r2)
           (-> (h2-query
                ["SELECT rss_link_id FROM user_subscription WHERE id = ?"
                 (:id us)]) first :rss_link_id)))))
