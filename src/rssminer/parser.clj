(ns rssminer.parser
  (:use [rssminer.time :only [to-ts]]
        clojure.pprint
        [rssminer.util :only [assoc-if]]
        [clojure.tools.logging :only [error]])
  (:require [clojure.string :as str])
  (:import [com.sun.syndication.io SyndFeedInput]
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
             (to-ts ^Date target) target))))

(definline trim [^String s]
  `(when ~s (str/trim ~s)))

(defn- parse-entry [e]
  (assoc-if {}
            :author (-> e :author trim)
            :title (-> e :title trim)
            :summary (or
                      (-> e :contents first :value trim)
                      (-> e :description :value trim))
            :link (-> e :link trim)
            ;; :guid (-> e :uri trim)
            :categories (set (map #(-> % :name trim str/lower-case)
                                  (:categories e)))
            :updated_ts (:updatedDate e)
            :published_ts (:publishedDate e)))

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
      (catch Exception e
        (error e "parse rss error")))))
