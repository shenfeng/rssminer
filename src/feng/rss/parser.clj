(ns feng.rss.parser
  (use [clojure.walk :only [postwalk]]
       clojure.contrib.trace)
  (require [clojure.zip :as zip])
  (import [com.sun.syndication.feed.synd SyndEntry SyndFeed]
          [com.sun.syndication.io FeedException SyndFeedInput]))


(let [rome? (fn [e] (re-find #"syndication" (str (class e))))]
  (defn- decode-bean [c]
    (let [target (if (rome? c) (bean c) c)]
      (cond
       (map? target)
       (into {}
             (for [[k v] target]
               [(keyword k) (decode-bean v)]))
       (vector? target)
       (vec (map decode-bean target))
       :else target))))

(defn parse [file]
  (let [feed (.build (SyndFeedInput.) file)]
    (map decode-bean (trace (.getEntries feed)))))


