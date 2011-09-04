(ns rssminer.views.dashboard-test
  (:use clojure.test
        (clojure.data [json :only [read-json json-str]])
        (rssminer [test-common :only [test-app h2-fixture]])))

(use-fixtures :each h2-fixture)

(defn- make-req [q]
  (test-app {:uri "/api/dashboard/"
             :params {"q" q}
             :request-method :get}))

(deftest test-get-rsslinks-stats
  (let [resp (make-req "rsslinks")
        stats (-> resp :body read-json)]
    (is (= 200 (:status resp)))
    (is (seq (-> stats :data)))))

(deftest test-get-pending
  (let [resp (make-req "pending")
        stats (-> resp :body read-json)]
    (is (= 200 (:status resp)))
    (is (seq (-> stats :data)))))

(deftest test-get-crawled
  (let [resp (make-req "crawled")
        stats (-> resp :body read-json)]
    (is (= 200 (:status resp)))
    (is (empty? (-> stats :data)))))

(deftest test-get-settings
  (let [resp (make-req "settings")
        settings (-> resp :body read-json)]
    (is (= 200 (:status resp)))
    (is (> (count (:black_domains settings)) 0))
    (is (> (count (:reseted_domains settings)) 0))))

(comment
  (deftest test-add-modify-settings
    (let [resp (test-app {:uri "/api/dashboard/"
                          :request-method :post
                          :body (json-str {:patten "test"})})
          body (-> resp :body read-json)]
      (is (= 200 (:status resp)))
      (is (some #(= "test" %) body)))))

