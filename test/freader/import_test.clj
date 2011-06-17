(ns freader.import-test
  (:use clojure.test
        [clojure.java.io :only [resource]]
        freader.import))

(def test-opml (slurp (resource "opml.xml")))

(deftest test-parse-opml
  (let [result (parse-opml test-opml)]
    (is (= 11 (count result)))
    (are [d] (-> result first :subscriptions first)
         :title
         :link
         :type)))
