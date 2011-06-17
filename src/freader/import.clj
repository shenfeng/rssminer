(ns freader.import
  (:use [clojure.java.io :only [resource]])
  (:require [clojure.xml :as xml]))

(defn parse-xml [s]
  (xml/parse
   (new org.xml.sax.InputSource
        (new java.io.StringReader s))))

(defn- value [a]
  (cond (empty? a) nil
        (or (seq? a) (vector? a)) (if (= 1 (count a))
                                    (value (first a)) a)
        :else a))

(defn- path [x tag]
  (let [r (map :content (filter #(= tag (:tag %)) x))]
    (value r)))

(defn- attr [x attr]
  (-> x :attrs attr))

(defn- extract [outline]
  {:title (attr outline :title)
   :link (attr outline :xmlUrl)
   :type (attr outline :type)})

(defn parse-opml [str]
  (let [d (:content (parse-xml str))
        outlines (path d :body)]
    (map (fn [outline]
           (let [subs (:content outline)]
             {:title (attr outline :title)
              :subscriptions
              (map extract  subs)})) outlines)))
