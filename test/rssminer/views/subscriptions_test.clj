(ns rssminer.views.subscriptions-test
  (:use clojure.test
        [clojure.data.json :only [read-json]]
        [rssminer.database :only [mysql-query mysql-insert]]
        (rssminer [test-common :only [auth-app auth-app2 app-fixture
                                      user1 json-body]]
                  [util :only [now-seconds]])))

(use-fixtures :each app-fixture)

(defn add-req [] {:uri "/api/subs/add"
                  :request-method :post
                  :body (json-body {:link "http://link-to-scottgu/rss"})})

(defn- prepare []
  (let [resp (auth-app (add-req))
        subscription (-> resp :body read-json)]
    (mysql-insert :user_feed {:user_id (:id user1)
                              :rss_link_id (:rss_link_id subscription)})
    [resp subscription
     (mysql-query ["select * from user_subscription"])
     (mysql-query ["select * from user_feed"])]))

(deftest test-add-feedsource
  (let [c (count (mysql-query ["select * from rss_links"]))
        [subscribe-resp subscription] (prepare)
        subscribe-again (auth-app (add-req))
        another-resp (auth-app2 (add-req))]
    (is (= 200 (:status subscribe-resp)))
    (is (= 200 (:status subscribe-again)))
    (is (= 200 (:status another-resp)))
    ;;    make sure only one subscription is added
    (is (= 1 (- (count (mysql-query ["select * from rss_links"])) c)))
    (let [rss (first (mysql-query
                      ["select * from rss_links order by id desc"]))]
      (is (>  (now-seconds) (:next_check_ts rss))))
    (are [key] (-> subscription key)
         :id
         :user_id
         :rss_link_id)))

(deftest test-get-subscription
  (let [[_ subscription] (prepare)
        rss-id (:rss_link_id subscription)]
    (doseq [s ["newest" "oldest" "recommend" "read" "voted"]]
      (let [resp (auth-app {:uri (str "/api/subs/" rss-id)
                            :request-method :get
                            :params {"limit" "13" "offset" "0" "sort" s}})
            resp2 (auth-app {:uri (str "/api/subs/" rss-id "-" (inc rss-id))
                             :request-method :get
                             :params {"limit" "13" "offset" "0" "sort" s}})]
        ;; test has some data
        (is (empty? (-> resp :body read-json)))
        (is (= 200 (:status resp)))
        (is (empty? (-> resp2 :body read-json)))
        (is (= 200 (:status resp2)))))))

(deftest test-list-subscription
  (is (= 200 (:status (auth-app {:uri "/api/subs"
                                 :request-method :get})))))

(deftest test-unsubscripe
  (let [[_ subscription subscriptions user-feeds] (prepare)
        delete-resp (auth-app {:uri (str "/api/subs/"
                                         (:rss_link_id subscription))
                               :request-method :delete})]
    (is (= 200 (:status delete-resp)))
    (is (= 1 (- (count subscriptions)
                (count (mysql-query ["select * from user_subscription"])))))
    (is (nil? (mysql-query ["select * from user_feed where rss_link_id = ?"
                            (:rss_link_id subscription)])))))

(deftest test-poll-fetcher
  (let [[_ subscription] (prepare)
        resp (auth-app {:uri (str "/api/subs/p/" (:rss_link_id subscription))
                        :request-method :get})]
    (is (= (:rss_link_id subscription) (-> resp :body read-json :id)))
    (is (= 200 (:status resp)))))
