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
  (testing "filter sex site by keyword"
    (is (black-domain? "adultsexplay.com"))
    (is (black-domain? "http://www.naughtypeekcams.com"))
    (is (black-domain? "http://atriohotxxxx.adultchatbabes.com"))
    (is (black-domain? "http://www.jasminspycam.com"))
    (is (black-domain? "http://grinder.a-livesexasian.com"))
    (is (black-domain? "http://www.pussypolice.com"))
    (is (black-domain? "http://naughtyscreenshots.com"))
    (is (black-domain? "http://suckmehere.com"))
    (is (black-domain? "http://www.hornyvideomodels.com"))
    (is (black-domain? "http://www.joyfeeds.com"))
    (is (black-domain? "http://www.cammodelprofiles.com")))
  (is (black-domain? "www.tumblr.com"))
  (is (black-domain? "http://antwerpen.paginamarkt.nl"))
  (is (black-domain? "http://g.cn"))
  (is (black-domain? "http://eroda.xin.taobao.com"))
  (is (black-domain? "http://wap.sohu.com"))
  (is (black-domain? "http://gdqc.house.inhe.net"))
  (is (black-domain? "http://www.skyrock.com"))
  (is (black-domain? "http://jeunefillebien.canalblog.com"))
  (is (black-domain? "http://francescaferdi.forumcommunity.net"))
  (is (black-domain? "http://news.ycombinator.com"))
  (is (black-domain? "http://www.blshe.com"))
  (is (black-domain? "http://www.linkinpark.com"))
  (is (black-domain? "http://shop.ea3w.com"))
  (is (black-domain? "http://www.soufun.com"))
  (is (black-domain? "http://timtimsia.over-blog.com"))
  (is (black-domain? "https://site.com"))
  (is (black-domain? "http://birsugibi01.blogcu.com"))
  (is (black-domain? "www.917lc.com"))
  (is (black-domain? "http://housebbs.sina.com.cn"))
  (is (black-domain? "http://sports.dzwww.com"))
  (is (not (black-domain? "http://google.com")))
  (add-black-domain-patten "google\\.com")
  (is (black-domain? "http://google.com")))

