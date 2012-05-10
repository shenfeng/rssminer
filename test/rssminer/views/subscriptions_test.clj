(ns rssminer.views.subscriptions-test
  (:use clojure.test
        [clojure.data.json :only [read-json]]
        [rssminer.db.util :only [mysql-query mysql-insert]]
        (rssminer [test-common :only [auth-app auth-app2 app-fixture
                                      user1 json-body]]
                  [time :only [now-seconds]])))

(use-fixtures :each app-fixture)

(defn add-req [] {:uri "/api/subs/add"
                  :request-method :post
                  :body (json-body {:link "http://link-to-scottgu/rss"})})

(defn- prepare []
  (let [resp (auth-app (add-req))
        subscription (-> resp :body read-json)]
    (mysql-insert :user_feed {:user_id (:id user1)
                              :rss_link_id (:rss_link_id subscription)})
    [resp subscription]))

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
        resp (auth-app {:uri (str "/api/subs/" (:rss_link_id subscription))
                        :request-method :get
                        :params {"limit" "13" "offset" "0"}})
        fetched-feeds (-> resp :body read-json)]
    (is (= 200 (:status resp)))))

(deftest test-unsubscripe
  (let [[_ subscription] (prepare)
        delete-resp (auth-app {:uri (str "/api/subs/"
                                         (:rss_link_id subscription))
                               :request-method :delete})]
    (is (= 200 (:status delete-resp)))
    (is (nil? (mysql-query ["select * from user_subscription"])))
    (is (nil? (mysql-query ["select * from user_feed"])))))
