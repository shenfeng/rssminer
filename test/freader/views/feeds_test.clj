(ns freader.views.feeds-test
  (:use clojure.test
        [clojure.contrib.json :only [read-json json-str]]
        (freader [middleware :only [*user*]]
                 [test-common :only [auth-app mock-http-get]]
                 [test-util :only [postgresql-fixture]]
                 [util :only [http-get get-favicon]])))

(use-fixtures :each postgresql-fixture
              (fn [f] (binding [http-get mock-http-get
                               get-favicon (fn [link] "icon")]
                       (f))))

(def add-req {:uri "/api/subscription"
              :request-method :post
              :body (json-str {:link "http://link-to-scottgu's rss"})})

(deftest test-add-feedsource
  (let [subscribe-resp (auth-app add-req)
        subscribe-again (auth-app add-req)
        subscription (-> subscribe-resp :body read-json)
        ;; fetch to make sure it is inserted to database
        fetch-resp (auth-app {:uri (str "/api/subscription/" (:id subscription))
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

(deftest test-get-overview
  (let [subscribe-resp (auth-app add-req)
        overview-resp (auth-app {:uri "/api/overview"
                                 :request-method :get})
        overview (-> overview-resp :body read-json)]
    (is (= 200 (:status overview-resp)))
    (is (= 1 (count overview)))
    (are [key] (-> overview first key)
         :group_name
         :subscriptions)
    (are [key] (-> overview first :subscriptions first key)
         :id
         :total_count
         :total_count
         :title
         :favicon)))

(deftest test-customize-subscription
  (let [subscribe (-> (auth-app add-req) :body read-json)
        new-group "just-new-group"
        new-title "fancy title"
        modify-req {:uri (str "/api/subscription/" (:id subscribe))
                    :request-method :post
                    :body (json-str {:group_name new-group
                                     :title new-title})}
        resp (auth-app modify-req)
        overview (-> (auth-app {:uri "/api/overview"
                                :request-method :get}) :body read-json)]
    (is (= 200 (:status resp)))
    (is (= new-title
           (-> resp :body read-json :title)
           (-> overview first :subscriptions first :title)))
    (is (= new-group
           (-> resp :body read-json :group_name)
           (-> overview first :group_name)))
    (is (= (:id subscribe) (-> resp :body read-json :id)))))
