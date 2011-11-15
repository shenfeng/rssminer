(ns rssminer.views.subscriptions-test
  (:use clojure.test
        [clojure.data.json :only [read-json json-str]]
        [rssminer.db.util :only [h2-query select-sql-params]]
        [rssminer.time :only [now-seconds]]
        (rssminer [test-common :only [auth-app auth-app2 app-fixture]]
                  [http :only [download-rss]])))

(use-fixtures :each app-fixture
              (fn [f] (binding [download-rss
                               (fn [& args]
                                 {:body (slurp "test/ppurl-rss.xml")})]
                       (f))))

(def add-req {:uri "/api/subs/add"
              :request-method :post
              :body (json-str {:link "http://link-to-scottgu/rss"})})

(defn- prepare []
  (let [resp (auth-app add-req)
        subscription (-> resp :body read-json)]
    [resp subscription]))

(deftest test-add-feedsource
  (let [c (count (h2-query ["select * from rss_links"]))
        [subscribe-resp subscription] (prepare)
        subscribe-again (auth-app add-req)
        another-resp (auth-app2 add-req)]
    (is (= 200 (:status subscribe-resp)))
    (is (= 409 (:status subscribe-again)))
    (is (= 200 (:status another-resp)))
    ;;    make sure only one subscription is added
    (is (= 1 (- (count (h2-query ["select * from rss_links"])) c)))
    (let [rss (first (h2-query
                      ["select * from rss_links order by id desc"]))]
      (is (> (:next_check_ts rss) (now-seconds))))
    (are [key] (-> subscription key)
         :total_count
         :id
         :unread_count
         :title)))

(deftest test-get-subscription
  (let [[_ subscription] (prepare)
        resp (auth-app {:uri (str "/api/subs/" (:id subscription))
                        :request-method :get
                        :params {"limit" "13" "offset" "0"}})
        fetched-feeds (-> resp :body read-json)]
    (is (= 200 (:status resp)))
    (are [key] (-> fetched-feeds first key)
         :tags
         :comments
         :id
         :title)
    (is (= 1 (count fetched-feeds)))))

(deftest test-customize-subscription
  (let [[_ subscription] (prepare)
        new-group "just-new-group"
        new-title "fancy title"
        modify-req {:uri (str "/api/subs/" (:id subscription))
                    :request-method :post
                    :body (json-str {:group_name new-group
                                     :title new-title})}
        resp (auth-app modify-req)]
    (is (= 200 (:status resp)))))

(deftest test-delete-user-subscription
  (let [[_ subscription] (prepare)
        delete-resp (auth-app {:uri (str "/api/subs/"
                                         (:id subscription))
                               :request-method :delete})]
    (is (= 200 (:status delete-resp)))))
