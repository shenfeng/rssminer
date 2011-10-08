(ns rssminer.http-test
  (:refer-clojure :exclude [get])
  (:use rssminer.http
        clojure.test)
  (:import [org.jboss.netty.handler.codec.http DefaultHttpResponse
            HttpResponse HttpVersion HttpResponseStatus]
           org.jboss.netty.buffer.ChannelBuffers))

(deftest test-extract-host
  (is (= (extract-host "http://192.168.1.11:8000/#change,83")
         "http://192.168.1.11:8000"))
  (is (= (extract-host "https://github.com/master/books/src/trakr/routes.clj")
         "https://github.com")))

(defn- black-domain? [host]
  (nil? (clean-resolve host "")))

(deftest test-black-domain-patten
  (is (black-domain? "http://guangzhoufuzhuangsheyin04106333.sh-kbt.com"))
  (testing "no value site"
    (is (black-domain? "http://shippingport.pa.us.weatheradd.com"))
    (is (black-domain? "http://morgaine.jaiku.com"))
    (is (black-domain? "http://www.zzbbs.com"))
    (is (black-domain? "http://tsuki-shika.deviantart.com"))
    (is (black-domain? "http://clojure.meetup.com")))
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
  (is (black-domain? "http://www.linkinpark.com"))
  (is (black-domain? "http://shop.ea3w.com"))
  (is (black-domain? "http://www.soufun.com"))
  (is (black-domain? "http://timtimsia.over-blog.com"))
  (is (black-domain? "http://birsugibi01.blogcu.com"))
  (is (black-domain? "www.917lc.com"))
  (is (black-domain? "http://housebbs.sina.com.cn"))
  (is (black-domain? "http://sports.dzwww.com"))
  (is (not (black-domain? "http://google.com"))))

(deftest test-clean-resolve
  (is (= "http://a.com/c.html"
         (str (clean-resolve "http://a.com/index?a=b" "c.html"))))
  (is (= "http://a.com/c.html?a=b"
         (str (clean-resolve "http://a.com/index?a=b" "c.html?a=b"))))
  (is (= "http://a.com/rss.html"
         (str (clean-resolve "http://a.com" "rss.html"))))
  (is (= "http://a.com/c.html"
         (str (clean-resolve "http://a.com/b.html" "c.html"))))
  (is (= "http://a.com/a/c.html"
         (str (clean-resolve "http://a.com/a/b/" "../c.html"))))
  (is (= "http://c.com/c.html"
         (str (clean-resolve "http://a.com/" "http://c.com/c.html")))))

(deftest test-extract-links
  (let [html (slurp "test/page.html")
        {:keys [rss links title]} (extract-links "http://a.com/" html)]
    (is (> (count links) 0))
    (is (= title "Peter Norvig"))
    (is (every? #(and (:url %)
                      (:domain %)) links))
    (are [k] (-> rss first k)
         :title
         :url)
    (is (= 1 (count rss)))))

(deftest test-parse-responce
  (let [resp (doto (DefaultHttpResponse. HttpVersion/HTTP_1_1
                     HttpResponseStatus/OK)
               (.setHeader "H1" "v1")
               (.setHeader "H2" "v2")
               (.setContent
                (ChannelBuffers/copiedBuffer (.getBytes "test body"))))
        r (parse-response resp)]
    (is (= 200 (:status r)))
    (is (= "v1" (-> r :headers :h1)))
    (is (= "v2" (-> r :headers :h2)))
    (is (= "test body" (:body r)))))
