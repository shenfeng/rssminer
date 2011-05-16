(ns feng.rss.parser-test
  (use clojure.pprint
       clojure.test
       feng.rss.parser)
  (require [clojure.zip :as zip]
           [clojure.contrib.lazy-xml :as xml]))

(def rss1 "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>
<rss version=\"2.0\">
<channel>
        <title>RSS Title</title>
        <description>This is an example of an RSS feed</description>
        <link>http://www.someexamplerssdomain.com/main.html</link>
        <lastBuildDate>Mon, 06 Sep 2010 00:01:00 +0000 </lastBuildDate>
        <pubDate>Mon, 06 Sep 2009 16:45:00 +0000 </pubDate>
 
        <item>
                <title>Example entry</title>
                <description>Here is some text containing an interesting description.</description>
                <link>http://www.wikipedia.org/</link>
                <guid>unique string per item</guid>
                <pubDate>Mon, 06 Sep 2009 16:45:00 +0000 </pubDate>
        </item>
 
</channel>
</rss>")

(deftest test-parse-1
  (let [sr (java.io.StringReader. rss1)
        rss (parse sr)]
    (is (= "RSS Title" (:title rss)))
    (is (= "This is an example of an RSS feed" (:description rss)))
    (is (= 1 (count (:feeds rss))))))
