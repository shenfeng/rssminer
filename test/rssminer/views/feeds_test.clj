(ns rssminer.views.feeds-test
  (:use clojure.test
        rssminer.db.feed
        [clojure.data.json :only [json-str]]
        [rssminer.db.util :only [mysql-query with-mysql mysql-insert-and-return]]
        (rssminer [test-common :only [user1 app-fixture auth-app
                                      mk-feeds-fixtrue]])))

(use-fixtures :each app-fixture (mk-feeds-fixtrue "test/scottgu-atom.xml"))

(deftest test-user-vote
  (let [fid (-> (mysql-query ["select id from feeds"]) first :id)
        resp (auth-app {:uri (str "/api/feeds/" fid "/vote")
                        :request-method :post
                        :body (json-str {"vote" "1"})})]
    (is (= 204 (:status resp)))
    (is (= 1 (-> (mysql-query
                  ["SELECT vote_user FROM user_feed WHERE
                    user_id = ? AND feed_id = ?" (:id user1) fid])
                 first :vote_user)))))

