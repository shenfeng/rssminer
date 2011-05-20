(ns feng.rss.parser
  (import [com.sun.syndication.io SyndFeedInput]))

(defn- rss-bean?
  [e]
  (re-find #"sun" (str (class e))))

(defn- list-like? [o]
  (or (vector? o)
      (list? o)
      (instance? java.util.List o)))

(defn- keep? [e]
  (not (or (nil? e)
           (when (seq? e) 
             (empty? e)))))

(defn- decode-bean [c]
  (let [target (if (rss-bean? c) (bean c) c)]
    (cond
     (map? target)
     (into {}
           (for [[k v] target
                 d (list (decode-bean v))
                 :when (keep? d)]
             [k d]))
     (list-like? target)
     (map decode-bean target)
     :else target)))

(defn parse [file]
  (let [feed (.build (SyndFeedInput.) file)]
    (decode-bean feed)))


