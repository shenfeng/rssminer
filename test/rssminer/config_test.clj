(ns rssminer.config-test
  (:use rssminer.config
        clojure.test))

(deftest test-reseted-url
  (is (reseted-url? "emacs-fu.blogspot.com/"))
  (is (not (reseted-url? "http://google.com"))))

(deftest test-muitl-domain
  (is (multi-domain? "blogs.oracle.com"))
  (is (not (multi-domain? "rssminer.net"))))
