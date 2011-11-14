(ns rssminer.search-test
  (:use clojure.test
        (rssminer [test-common :only [app-fixture mk-feeds-fixtrue]]
                  [search :only [search*]])))

(use-fixtures :each app-fixture (mk-feeds-fixtrue "test/scottgu-atom.xml"))

(deftest test-search
  (testing "search summary"
    (let [resp (search* "onsummary" 10)]
      (is (= (count resp) 1))))
  (testing "search category"
    (let [resp (search* "tag:acategory" 10)]
      (is (= (count resp) 1))))
  (testing "search author"
    (let [resp (search* "author:aScottGu" 10)]
      (is (= (count resp) 1)))))
