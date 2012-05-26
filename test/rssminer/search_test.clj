(ns rssminer.search-test
  (:use clojure.test
        (rssminer [test-common :only [app-fixture mk-feeds-fixtrue user1
                                      auth-app]]
                  [search :only [search*]])))

(use-fixtures :each app-fixture (mk-feeds-fixtrue "test/scottgu-atom.xml"))

(deftest test-search
  (let [rss-ids (range 1 100)
        ids (apply str (interpose "," rss-ids))
        user-id (:id user1)]
    (testing "search summary"
      (let [resp (:body (search* "onsummary" user-id rss-ids 10))]
        (is (= (count resp) 1))))
    (testing "search category"
      (let [resp (:body (search* "acategory" user-id rss-ids 10))]
        (is (= (count resp) 1))))
    (testing "search author"
      (let [resp (:body (search* "aScottGu" user-id rss-ids 10))
            r (auth-app {:uri "/api/search"
                         :request-method :get
                         :params {"q" "aScottGu" "ids" ids "limit" 10}})]
        (is (= (count resp) 1))
        (is (= 200 (:status r)))))))
