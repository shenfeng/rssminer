(ns rssminer.views.dashboard-test
  (:use clojure.test
        (clojure.data [json :only [read-json json-str]])
        (rssminer [test-common :only [test-app h2-fixture]])))

(use-fixtures :each h2-fixture)

(deftest test-get-rsslinks-stats
  (let [resp (test-app {:uri "/api/dashboard/rsslinks"
                        :request-method :get})
        stats (-> resp :body read-json)]
    (is (= 200 (:status resp)))
    (is (empty?  (-> stats :rss_links)))
    (are [k] (-> stats k)
         :total_count
         :rss_links_cout
         :crawled_count)))

(deftest test-get-pending
  (let [resp (test-app {:uri "/api/dashboard/pending"
                        :request-method :get})
        stats (-> resp :body read-json)]
    (is (= 200 (:status resp)))
    (is (seq (-> stats :pending_links)))))

(deftest test-get-crawled
  (let [resp (test-app {:uri "/api/dashboard/crawled"
                        :request-method :get})
        stats (-> resp :body read-json)]
    (is (= 200 (:status resp)))
    (is (empty? (-> stats :crawled_links)))))

(deftest test-get-black-patten
  (let [resp (test-app {:uri "/api/dashboard/black"
                        :request-method :get})
        stats (-> resp :body read-json)]
    (is (= 200 (:status resp)))))

(deftest test-add-black-patten
  (let [resp (test-app {:uri "/api/dashboard/black"
                        :request-method :post
                        :body (json-str {:patten "test"})})
        body (-> resp :body read-json)]
    (is (= 200 (:status resp)))
    (is (some #(= "test" %) body))))

