(ns rssminer.search-test
  (:use clojure.test
        (rssminer [test-common :only [app-fixture mk-feeds-fixtrue]]
                  [search :only [search*]])))

(use-fixtures :each app-fixture (mk-feeds-fixtrue "test/scottgu-atom.xml"))

(deftest test-search
  (let [rss-ids (range 1 100)]
    (testing "search summary"
      (let [resp (search* "onsummary" rss-ids 10)]
        (is (= (count resp) 1))))
    (testing "search category"
      (let [resp (search* "acategory" rss-ids 10)]
        (is (= (count resp) 1))))
    (testing "search author"
      (let [resp (search* "aScottGu" rss-ids 10)]
        (is (= (count resp) 1))))))
