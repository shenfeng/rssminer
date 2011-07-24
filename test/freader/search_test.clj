(ns freader.search-test
  (:use clojure.test
        [clojure.data.json :only [read-json json-str]]
        (freader [test-common :only [auth-app mock-download-feed-source
                                     app-fixture]]
                 [util :only [download-favicon download-feed-source]])))

(defn- prepare [f]
  (binding [download-feed-source mock-download-feed-source
            download-favicon (fn [link] "icon")]
    (auth-app {:uri "/api/subscriptions/add"
               :request-method :post
               :body (json-str {:link "http://link-to-scottgu's rss"})})
    (f)))

(use-fixtures :each app-fixture prepare)

(deftest test-search
  (let [resp (auth-app {:uri "/api/feeds/search"
                        :request-method :get
                        :params {"term" "mvc"}})]
    (is (= 200 (:status resp)))
    (is (> (-> resp :body read-json count) 1))))
