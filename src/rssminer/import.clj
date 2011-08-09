(ns rssminer.import
  (:use [rssminer.handlers.subscriptions :only [add-subscription*]]
        [rssminer.middleware :only [*user*]])
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

(defn parse-opml [str]
  (let [d (:content (parse-xml str))
        outlines (path d :body)]
    (mapcat (fn [outline]
              (let [subs (:content outline)
                    group-name (attr outline :title)]
                (map (fn [sub]
                       {:title (attr sub :title)
                        :link (attr sub :xmlUrl)
                        :group_name group-name
                        :type (attr sub :type)}) subs))) outlines)))

(defn opml-import [req]
  (let [file (-> req :params :file :tempfile)]
    (if (and file (> (.length file) 10))
      (let [user-id (:id *user*)
            subs (parse-opml (slurp file))]
        (map #(add-subscription* (:link %) user-id
                                 :group_name (:group_name %)
                                 :title (:title %)) subs))
      {:status 400
       :message "Please choose a file"})))
