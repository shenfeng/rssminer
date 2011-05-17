(ns feng.rss.parser
  (use [clojure.walk :only [postwalk]]
       clojure.contrib.trace)
  (require [clojure.zip :as zip])
  (import [com.sun.syndication.feed.synd SyndEntry SyndFeed]
          [com.sun.syndication.io FeedException SyndFeedInput]))

(defn- rss-bean?
  [e]
  (re-find #"sun" (str (class e))))

(defn- list-like? [o]
  (or (vector? o)
      (list? o)
      (instance? java.util.List o)))

(defn- decode-bean [c]
  (let [target (if (rss-bean? c) (bean c) c)]
    (cond
     (map? target)
     (into {}
           (for [[k v] target]
             [k  (decode-bean v)]))
     (list-like? target)
     (map decode-bean target)
     :else target)))

(defn parse [file]
  (let [feed (.build (SyndFeedInput.) file)]
    (decode-bean feed)))


