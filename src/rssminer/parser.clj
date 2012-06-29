(ns rssminer.parser
  (:use [rssminer.util :only [assoc-if now-seconds]]
        [clojure.tools.logging :only [error trace]])
  (:require [clojure.string :as s])
  (:import [com.sun.syndication.io SyndFeedInput ParsingFeedException]
           java.util.Date
           java.io.StringReader))

(defn- rss-bean? [e]
  (re-find #"sun" (str (class e))))

(defn- list-like? [o]
  (or (coll? 0) (instance? java.util.List o)))

(defn- keep? [e]
  (and e (if (seq? e)
           (some keep? e)
           true)))

(defn- decode-bean [c]
  (let [target (if (rss-bean? c) (bean c) c)]
    (cond
     (map? target) (into {}
                         (for [[k v] target d (list (decode-bean v))
                               :when (keep? d)]
                           [k d]))
     (list-like? target) (map decode-bean target)
     :else (if (instance? Date target)
             (quot (.getTime ^Date target) 1000) target))))

(definline trim [^String s]
  `(when ~s (s/trim ~s)))

(defn most-len [^String s len]
  (when s
    (if (> (.length s) len) (.substring s 0 len) s)))

;; http://hi.baidu.com/maczhijia/rss 0 feeds
;;; http://blogs.innodb.com/wp/feed/

(defn- parse-entry [e]
  (assoc-if {}
            :author (most-len (-> e :author trim) 64)
            :title (most-len (-> e :title trim) 256)
            :summary (or
                      (-> e :contents first :value trim)
                      (-> e :description :value trim))
            :link (most-len (-> e :link trim) 512) ; most 512 chars
            :tags (let [t (s/join "; " (map #(-> % :name trim)
                                            (:categories e)))]
                    (most-len t 128))
            :updated_ts (:updatedDate e)
            :published_ts (or (:publishedDate e)
                              (:updatedDate e)
                              (now-seconds))))

(defn parse-feed [str]
  (when str
    (try
      (let [feed (->> str StringReader.
                      (.build (SyndFeedInput.)) decode-bean)]
        {:title (-> feed :title trim)
         :link (-> feed :link trim)
         :language (-> feed :link trim)
         :published_ts (:publishedDate feed)
         :description (-> feed :description trim)
         :entries (map parse-entry (:entries feed))})
      (catch ParsingFeedException e
        (trace "ParsingFeedException" e))
      (catch Exception e
        (trace e "parse rss error")))))
