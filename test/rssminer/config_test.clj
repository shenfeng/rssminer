(ns rssminer.config-test
  (:use rssminer.config
        clojure.test))

(deftest test-reseted-url
  (is (reseted-url? "emacs-fu.blogspot.com/"))
  (is (not (reseted-url? "http://google.com"))))

(deftest test-black-domain-patten
  (is (black-domain? "http://guangzhoufuzhuangsheyin04106333.sh-kbt.com"))
  (testing "no value site"
    (is (black-domain? "http://shippingport.pa.us.weatheradd.com")))
  (testing "filter sex site by keyword"
    (is (black-domain? "adultsexplay.com"))
    (is (black-domain? "http://www.erotikexpress.com"))
    (is (black-domain? "http://www.naughtypeekcams.com"))
    (is (black-domain? "http://atriohotxxxx.adultchatbabes.com"))
    (is (black-domain? "http://www.jasminspycam.com"))
    (is (black-domain? "http://grinder.a-livesexasian.com"))
    (is (black-domain? "http://www.pussypolice.com"))
    (is (black-domain? "http://naughtyscreenshots.com"))
    (is (black-domain? "http://suckmehere.com"))
    (is (black-domain? "http://schoolspirit.thumblogger.com"))
    (is (black-domain? "http://chat-fuer-schwangere.1eurohardcore.com"))
    (is (black-domain? "http://chat-ausziehen-wichsen.1buckfatty.com"))
    (is (black-domain? "http://www.hornyvideomodels.com"))
    (is (black-domain? "http://www.freeswingersguide.com"))
    (is (black-domain? "http://www.mature-post.com"))
    (is (black-domain? "http://www.joyfeeds.com"))
    (is (black-domain? "http://www.cammodelprofiles.com")))
  (testing "filter blog site that has too many domain"
    (is (black-domain? "www.tumblr.com"))
    (is (black-domain? "www.artelista.com"))
    (is (black-domain? "www.mihanblog.com"))
    (is (black-domain? "www.fotopages.com"))
    (is (black-domain? "www.blogfa.com"))    ; not en, ch
    (is (black-domain? "www.Parsiblog.com")) ; not en, ch
    (is (black-domain? "www.blogsky.com"))   ; not en, ch
    (is (black-domain? "www.xanga.com")))
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
  (is (not (black-domain? "http://google.com"))))

(deftest test-muitl-domain
  (is (multi-domain? "http://blogs.oracle.com"))
  (is (not (multi-domain? "http://rssminer.net"))))
