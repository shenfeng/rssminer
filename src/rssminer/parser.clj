(ns rssminer.parser
  (import [com.sun.syndication.io SyndFeedInput]
          java.sql.Timestamp
          java.util.Date))

(defn- rss-bean?
  [e]
  (re-find #"sun" (str (class e))))

(defn- list-like? [o]
  (or (vector? o)
      (list? o)
      (instance? java.util.List o)))

(defn- keep? [e]
  (and e
       (if(seq? e)
         (some keep? e)
         true)))

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
     :else (if (instance? Date target)
             (Timestamp. (.getTime target))
             target))))

(defn parse [str]
  (try
    (let [input (java.io.StringReader. str)
          feed (.build (SyndFeedInput.) input)]
      (decode-bean feed))
    (catch Exception e)))


