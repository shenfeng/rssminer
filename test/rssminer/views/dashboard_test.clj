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
    (is (empty?  (-> stats :rss_links)))))

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

(deftest test-get-settings
  (let [resp (test-app {:uri "/api/dashboard/settings"
                        :request-method :get})
        settings (-> resp :body read-json)]
    (is (= 200 (:status resp)))
    (is (> (count (:black_domains settings)) 0))
    (is (> (count (:reseted_domains settings)) 0))))

(deftest test-add-modify-settings
  (let [resp (test-app {:uri "/api/dashboard/settings"
                        :request-method :post
                        :body (json-str {:patten "test"})})
        body (-> resp :body read-json)]
    (is (= 200 (:status resp)))
    (is (some #(= "test" %) body))))

