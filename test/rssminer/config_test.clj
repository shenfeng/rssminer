(ns rssminer.config-test
  (:use rssminer.config
        clojure.test))

(deftest test-muitl-domain
  (is (multi-domain? "blogs.oracle.com"))
  (is (not (multi-domain? "rssminer.net"))))
