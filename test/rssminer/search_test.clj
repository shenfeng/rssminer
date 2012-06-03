(ns rssminer.search-test
  (:use clojure.test
        (rssminer [test-common :only [app-fixture mk-feeds-fixtrue user1
                                      auth-app]]
                  [util :only [assoc-if]])
        [clojure.data.json :only [read-json]]))

(use-fixtures :each app-fixture (mk-feeds-fixtrue "test/scottgu-atom.xml"))

(defn- do-search [term subids]
  (let [params (assoc-if  {"q" term  "limit" 10} "ids" subids)]
    (auth-app {:uri "/api/search"
               :request-method :get
               :params params})))

(deftest test-search
  (let [rss-ids (range 1 100)
        ids (apply str (interpose "," rss-ids))]
    (doseq [term ["onsummary" "acategory" "aScottGu"]]
      (let [resp (do-search term nil)]
        (is (= 200 (:status resp)))
        (is (= 1 (count (-> resp :body read-json)))))
      (let [resp (do-search term (apply str (interpose "," (range 1 100))))]
        (is (= 200 (:status resp)))
        (is (= 1 (count (-> resp :body read-json))))))))
