(ns rssminer.db.subscripton-test
  (:use clojure.test
        rssminer.db.subscription
        [rssminer.database :only [mysql-query with-mysql
                                  mysql-insert-and-return]]
        [clojure.java.jdbc :only [delete-rows]]
        [rssminer.test-common :only [user1 app-fixture user2
                                     mk-feeds-fixtrue]]))

(use-fixtures :each app-fixture (mk-feeds-fixtrue "test/scottgu-atom.xml"))

(def rss1 {:url "http://link1.com"})

(def rss2 {:url "http://link2.com"})

(deftest test-update-rss-link-simple
  (let [r1 (mysql-insert-and-return :rss_links rss1)
        newly (assoc rss2 :title "aaa")
        newly2 {:title "aaa"}]
    (update-rss-link (:id r1) newly)
    (is (= newly (first (mysql-query ["SELECT url, title FROM rss_links
                                       WHERE id =?" (:id r1)]))))
    (update-rss-link (:id r1) newly2)
    (is (= newly2 (first (mysql-query ["SELECT title FROM rss_links
                                       WHERE id =?" (:id r1)]))))))
;;; simple update rss_link
(deftest test-update-rss-link
  (let [r1 (mysql-insert-and-return :rss_links rss1)
        r2 (mysql-insert-and-return :rss_links rss2)
        us (mysql-insert-and-return :user_subscription
                                    {:user_id (:id user1)
                                     :rss_link_id (:id r1)})]
    (update-rss-link (:id r1) rss2)
    (is (nil? (mysql-query ["SELECT * FROM rss_links WHERE url = ?"
                            (:url rss1)])))
    (is (= (:id r2)
           (-> (mysql-query ["SELECT * FROM user_subscription WHERE id = ?"
                             (:id us)]) first :rss_link_id)))))

(deftest test-update-rss-link2
  (let [r1 (mysql-insert-and-return :rss_links rss1)
        us (mysql-insert-and-return :user_subscription
                                    {:user_id (:id user1)
                                     :rss_link_id (:id r1)})]
    (update-rss-link (:id r1) rss2)
    (is (nil? (mysql-query ["SELECT * FROM rss_links WHERE url = ?"
                            (:url rss1)])))
    (is (= (:url rss2) (-> (mysql-query ["SELECT * FROM rss_links
                                           WHERE id = ?" (:id r1)])
                           first :url)))))

(deftest test-update-rss-link-complex
  (let [r1 (mysql-insert-and-return :rss_links rss1)
        r2 (mysql-insert-and-return :rss_links rss2)
        us (mysql-insert-and-return :user_subscription
                                    {:user_id (:id user1)
                                     :rss_link_id (:id r1)})
        u2s (mysql-insert-and-return :user_subscription
                                     {:user_id (:id user2)
                                      :rss_link_id (:id r1)})
        us2 (mysql-insert-and-return :user_subscription
                                     {:user_id (:id user1)
                                      :rss_link_id (:id r2)})]
    (update-rss-link (:id r1) rss2)
    ;; no rss1 anymore
    (is (= nil
           (mysql-query ["SELECT id FROM rss_links WHERE url = ?"
                         "http://link1.com"])
           (mysql-query ["select * from user_subscription where rss_link_id = ?"
                         (:id r1)])))
    (is (= (:id r2)
           (-> (mysql-query ["select * from user_subscription where id = ?"
                             (:id u2s)]) first :rss_link_id)
           (-> (mysql-query ["SELECT id from rss_links WHERE url = ?"
                             "http://link2.com"]) first :id)))))

(deftest test-fetch-rss-links
  (is (fetch-rss-links 10)))

