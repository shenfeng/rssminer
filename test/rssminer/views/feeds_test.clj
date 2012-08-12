(ns rssminer.views.feeds-test
  (:use clojure.test
        rssminer.db.feed
        [clojure.data.json :only [read-json]]
        [rssminer.database :only [mysql-query with-mysql
                                  mysql-insert-and-return]]
        (rssminer [test-common :only [user1 app-fixture auth-app json-body
                                      mk-feeds-fixtrue]])))

(use-fixtures :each app-fixture (mk-feeds-fixtrue "test/scottgu-atom.xml"))

(defn- vote [fid vote]
  (auth-app {:uri (str "/api/feeds/" fid "/vote")
             :request-method :post
             :body (json-body {"vote" vote})}))

(defn- get-user-feed [fid]
  (first (mysql-query
          ["SELECT * FROM user_feed WHERE
                    user_id = ? AND feed_id = ?" (:id user1) fid])))

(defn- first-feedid []
  (-> (mysql-query ["select id from feeds"]) last :id))

(deftest test-user-vote
  (let [fid (first-feedid)]
    (is (= 204 (:status (vote fid "1"))))
    (is (= 1 (:vote_user (get-user-feed fid))))
    (is (> (:vote_date (get-user-feed fid)) 0))
    ;; vote again
    (is (= 204 (:status (vote fid "-1"))))
    (is (= -1 (:vote_user (get-user-feed fid))))))

(deftest test-mark-as-read
  (let [fid (first-feedid)]
    ;; mark as read
    (is (= 204 (:status (auth-app {:uri (str "/api/feeds/" fid "/read")
                                   :request-method :post}))))
    (is (= (:vote_user (get-user-feed fid)) 0))
    ;; vote the just read feed
    (is (= 204 (:status (vote fid "1"))))
    ;; vote should saved
    (is (= (:vote_user (get-user-feed fid)) 1))
    ;; vote date should be updated
    (is (> (:vote_date (get-user-feed fid)) 1))))

(deftest test-fetch-feed
  (let [fid (first-feedid)
        resp (auth-app {:uri (str "/api/feeds/" fid)
                        :request-method :get})]
    (is (= (:status resp) 200))
    (is (-> resp :body read-json :summary))
    (is (nil? (mysql-query
               ["select * from user_feed where feed_id = ?" fid])) 0)
    (is (= (auth-app {:uri (str "/api/feeds/" fid)
                      :params {"read" "1"}
                      :request-method :get})))
    ;; mark as read
    (is (> (-> (mysql-query
                ["select * from user_feed where feed_id = ?" fid])
               first :read_date) 0))))

(deftest test-save-reading-time
  (let [ids (map :id (mysql-query ["select id from feeds"]))
        body (apply hash-map (interleave ids (range 238 400)))]
    (doseq [id ids]                     ; mark all as read
      (auth-app {:uri (str "/api/feeds/" id "/read")
                 :request-method :post}))
    (is (= 204 (:status (auth-app {:uri "/api/feeds"
                                   :request-method :post
                                   :body (json-body body)}))))
    (doseq [time (mysql-query ["select read_time from user_feed"])]
      (is (> (:read_time time) 0)))))

;;; list subs are in subscriptions_test
