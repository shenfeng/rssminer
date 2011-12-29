(ns rssminer.views.feeds-test
  (:use clojure.test
        rssminer.db.feed
        [clojure.data.json :only [json-str]]
        [rssminer.db.util :only [h2-query with-h2 h2-insert-and-return]]
        (rssminer [test-common :only [user1 app-fixture auth-app
                                      mk-feeds-fixtrue]])))

(use-fixtures :each app-fixture (mk-feeds-fixtrue "test/scottgu-atom.xml"))

(deftest test-user-vote
  (let [fid (-> (h2-query ["select id from feeds"]) first :id)
        resp (auth-app {:uri (str "/api/feeds/" fid "/vote")
                        :request-method :post
                        :body (json-str {"vote" "1"})})]
    (is (= 200 (:status resp)))
    (is (= 1 (-> (h2-query
                  ["SELECT vote FROM user_feed WHERE
                    user_id = ? AND feed_id = ?" (:id user1) fid])
                 first :vote)))))

