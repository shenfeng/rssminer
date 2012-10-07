(ns rssminer.parser
  (:use [rssminer.util :only [assoc-if now-seconds]]
        [clojure.tools.logging :only [error warn]])
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

(defn- tag? [^String s]
  (let [c (.length s)]
    (if (> c 1)
      true
      (when (and (= c 1) (> (int (.charAt s 0)) 255))
        true))))

;; http://hi.baidu.com/maczhijia/rss 0 feeds
;;; http://blogs.innodb.com/wp/feed/

(defn- parse-entry [e site-url]
  (let [link (most-len (or (-> e :link trim) site-url) 512)] ; most 512 chars
    (assoc-if {}
              :author (most-len (-> e :author trim) 64)
              :title (most-len (-> e :title trim) 256)
              :summary (or
                        (-> e :contents first :value trim)
                        (-> e :description :value trim))
              :link link
              :tags (let [t (s/join ";" (filter tag?
                                                (map #(-> % :name trim)
                                                     (:categories e))))]
                      (most-len t 128))
              :updated_ts (:updatedDate e)
              :published_ts (let [s (or (:publishedDate e)
                                        (:updatedDate e)
                                        (now-seconds))]
                              (if (< s 0) (now-seconds) s)))))

(defn parse-feed [str]
  (when str
    (try
      (let [feed (->> str StringReader.
                      (.build (SyndFeedInput.)) decode-bean)
            link (-> feed :link trim)]
        {:title (-> feed :title trim)
         :link link
         :language (-> feed :link trim)
         :published_ts (:publishedDate feed)
         :description (most-len (-> feed :description trim) 1024)
         :entries (map (fn [e]
                         (parse-entry e link)) (:entries feed))})
      ;; (catch ParsingFeedException e
      ;;   (warn "ParsingFeedException" (.getMessage e)))
      (catch Exception e
        (warn "parse feed exception:" (.getMessage e))))))
