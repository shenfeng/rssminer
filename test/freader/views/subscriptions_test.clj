(ns freader.views.subscriptions-test
  (:use clojure.test
        [clojure.contrib.json :only [read-json json-str]]
        (freader [test-common :only [auth-app mock-download-feed-source]]
                 [test-util :only [postgresql-fixture]]
                 [util :only [download-favicon download-feed-source]])))

(use-fixtures :each postgresql-fixture
              (fn [f] (binding [download-feed-source mock-download-feed-source
                               download-favicon (fn [link] "icon")]
                       (f))))

(def add-req {:uri "/api/subscriptions/add"
              :request-method :post
              :body (json-str {:link "http://link-to-scottgu's rss"})})

(defn- prepare []
  (let [resp (auth-app add-req)
        subscription (-> resp :body read-json)]
    [resp subscription]))

(deftest test-add-feedsource
  (let [[subscribe-resp subscription] (prepare)
        subscribe-again (auth-app add-req)
        ;; fetch to make sure it is inserted to database
        fetch-resp (auth-app {:uri (str "/api/subscriptions/"
                                        (:id subscription))
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
  (let [[subscribe-resp] (prepare)
        overview-resp (auth-app {:uri "/api/subscriptions/overview"
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
  (let [[_ subscription] (prepare)
        new-group "just-new-group"
        new-title "fancy title"
        modify-req {:uri (str "/api/subscriptions/" (:id subscription))
                    :request-method :post
                    :body (json-str {:group_name new-group
                                     :title new-title})}
        resp (auth-app modify-req)
        overview (-> (auth-app {:uri "/api/subscriptions/overview"
                                :request-method :get}) :body read-json)]
    (is (= 200 (:status resp)))
    (is (= new-title
           (-> resp :body read-json :title)
           (-> overview first :subscriptions first :title)))
    (is (= new-group
           (-> resp :body read-json :group_name)
           (-> overview first :group_name)))
    (is (= (:id subscription) (-> resp :body read-json :id)))))

(deftest test-delete-user-subscription
  (let [[_ subscription] (prepare)
        delete-resp (auth-app {:uri (str "/api/subscriptions/"
                                         (:id subscription))
                               :request-method :delete})
        overview (-> (auth-app {:uri "/api/subscriptions/overview"
                                :request-method :get}) :body read-json)]
    (is (= 200 (:status delete-resp)))
    (is (= 0 (count overview)))))
