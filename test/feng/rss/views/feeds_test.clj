(ns feng.rss.views.feeds-test
  (:use clojure.test
        [clojure.contrib.json :only [read-json json-str]]
        (feng.rss [middleware :only [*user*]]
                  [test-common :only [auth-app mock-http-get]]
                  [test-util :only [postgresql-fixture]]
                  [util :only [http-get get-favicon]])))

(use-fixtures :each postgresql-fixture
              (fn [f] (binding [http-get mock-http-get
                               get-favicon (fn [link] "icon")]
                       (f))))

(def add-subscription {:uri "/api/feeds"
                       :request-method :put
                       :body (json-str {:link "http://link-to-scottgu's rss"})})

(deftest test-add-feedsource
  (let [subscribe-resp (auth-app add-subscription)
        subscribe-again (auth-app add-subscription)
        subscription (-> subscribe-resp :body read-json)
        ;; fetch to make sure it is inserted to database
        fetch-resp (auth-app {:uri (str "/api/feeds/" (:id subscription))
                              :request-method :get
                              :params {"limit" "13"
                                       "offset" "0"}})
        fetched-feeds (-> fetch-resp :body read-json)]
    (are [key] (-> subscription key)
         :total_count
         :unread_count
         :favicon
         :title)
    (is (= 200 (:status subscribe-resp)))
    (is (= 200 (:status fetch-resp)))
    (is (= 409 (:status subscribe-again)))
    (are [key] (-> fetched-feeds :items first key)
         :categories
         :comments
         :id
         :title)
    (is (= 13 (count (:items fetched-feeds))))))

(deftest test-unread-count
  (let [subscribe-resp (auth-app add-subscription)
        unread-count-resp (auth-app {:uri "/api/unread-count"
                                     :request-method :get})
        unread-count (-> unread-count-resp :body read-json)]
    (is (= 200 (:status unread-count-resp)))
    (is (= 1 (count (:freader_ungrouped unread-count))))
    (is (= (-> unread-count :freader_ungrouped first :unread_count)
           (-> unread-count :freader_ungrouped first :total_count)))))
