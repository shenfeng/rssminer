(ns freader.views.dashboard-test
  (:use clojure.test
        (clojure.data [json :only [read-json]])
        (freader [test-common :only [test-app h2-fixture]])))

(use-fixtures :each h2-fixture)

(deftest test-get-crawler-stats
  (let [resp (test-app {:uri "/api/dashboard/crawler"
                        :request-method :get})
        stats (-> resp :body read-json)]
    (is (= 200 (:status resp)))
    (is (empty? (-> stats :crawled_links)))
    (are [k] (-> stats k)
         :total_count
         :rss_links
         :pending_links
         :crawled_count
         :crawled_links)))
