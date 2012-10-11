(ns rssminer.search-test
  (:use clojure.test
        (rssminer [test-common :only [app-fixture mk-feeds-fixtrue user1
                                      auth-app]]
                  [util :only [assoc-if]])
        [clojure.data.json :only [read-json]]))

(use-fixtures :each app-fixture (mk-feeds-fixtrue "test/scottgu-atom.xml"))

(deftest test-search
  (let [rss-ids (range 1 100)
        ids (apply str (interpose "," rss-ids))]
    (doseq [term ["onsummary" "acategory"]]
      (let [resp (auth-app {:uri "/api/search"
                            :request-method :get
                            :params {"q" term  "limit" 10}})]
        (is (= 200 (:status resp)))
        (is (= 1 (count (-> resp :body read-json :feeds))))))
    (let [resp (auth-app {:uri "/api/search"
                          :request-method :get
                          :params {"authors"  "aScottgu" "limit" 10}})]
      (is (= 200 (:status resp)))
      (is (= 1 (count (-> resp :body read-json :feeds)))))
    (let [resp (auth-app {:uri "/api/search"
                          :request-method :get
                          :params {"tags"  "acategory;mvc" "limit" 10}})]
      (is (= 200 (:status resp)))
      (is (= 1 (count (-> resp :body read-json :feeds)))))))
