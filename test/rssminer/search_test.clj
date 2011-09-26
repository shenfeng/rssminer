(ns rssminer.search-test
  (:use clojure.test
        [clojure.data.json :only [read-json json-str]]
        (rssminer [test-common :only [auth-app app-fixture]]
                  [http :only [download-favicon download-rss]]
                  [search :only [search*]])))

(defn- prepare [f]
  (binding [download-rss (fn [& args]
                           {:body (slurp "test/scottgu-atom.xml")})
            download-favicon (fn [link] "icon")]
    (auth-app {:uri "/api/subscriptions/add"
               :request-method :post
               :body (json-str {:link "http://link-to-scottgu's rss"})})
    (f)))

(use-fixtures :each app-fixture prepare)

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
