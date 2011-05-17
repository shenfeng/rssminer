(ns feng.rss.parser-test
  (use clojure.pprint
       clojure.test
       clojure.contrib.trace
       feng.rss.parser)
  (require [clojure.zip :as zip]
           [clojure.contrib.lazy-xml :as xml])
  (import [com.sun.syndication.feed.synd SyndEntry SyndFeed]
          [com.sun.syndication.io FeedException SyndFeedInput]))

(def rss1
  "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>
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

(def atom
  "<?xml version=\"1.0\" encoding=\"utf-8\"?>
<feed xmlns=\"http://www.w3.org/2005/Atom\">
         <title>Example Feed</title>
        <subtitle>A subtitle.</subtitle>
        <link href=\"http://example.org/feed/\" rel=\"self\" />
        <link href=\"http://example.org/\" />
        <id>urn:uuid:60a76c80-d399-11d9-b91C-0003939e0af6</id>
        <updated>2003-12-13T18:30:02Z</updated>
        <author>
                <name>John Doe</name>
                <email>johndoe@example.com</email>
        </author>
 
        <entry>
                <title>Atom-Powered Robots Run Amok</title>
                <link href=\"http://example.org/2003/12/13/atom03\" />
                <link rel=\"alternate\" type=\"text/html\" href=\"http://example.org/2003/12/13/atom03.html\"/>
                <link rel=\"edit\" href=\"http://example.org/2003/12/13/atom03/edit\"/>
                <id>urn:uuid:1225c695-cfb8-4ebb-aaaa-80da344efa6a</id>
                <updated>2003-12-13T18:30:02Z</updated>
                <summary>Some text.</summary>
        </entry>
</feed>")

(deftest test-rss
  (let [sr (java.io.StringReader. rss1)
        rss (parse sr)
        feeds (:entries rss)]
    (is (= "RSS Title" (:title rss)))
    (is (= "This is an example of an RSS feed" (:description rss)))
    (is (= 1 (count feeds)))
    (is (= "Example entry" (-> feeds first :title)))))


(deftest test-atom
  (let [sr (java.io.StringReader. atom)
        rss (parse sr)
        feeds (:entries rss)]
    (pprint rss)
    (is (= "Example Feed") (:title rss))
    (is (= 1 (count feeds)))))
