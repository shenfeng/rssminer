(ns rssminer.config-test
  (:use rssminer.config
        [clojure.java.io :only [resource]]
        (rssminer [test-common :only [h2-fixture]])
        clojure.test))

(use-fixtures :each h2-fixture)

(deftest test-reseted-url
  (is (reseted-url? "emacs-fu.blogspot.com/"))
  (is (not (reseted-url? "http://google.com")))
  (add-reseted-domain "http://google.com")
  (is (reseted-url? "http://google.com")))

(deftest test-black-domain-patten
  (is (black-domain? "http://guangzhoufuzhuangsheyin04106333.sh-kbt.com"))
  (is (black-domain? "www.tumblr.com/about"))
  (is (not (black-domain? "http://google.com")))
  (add-black-domain-patten "google\\.com")
  (is (black-domain? "http://google.com")))

