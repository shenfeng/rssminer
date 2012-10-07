(ns rssminer.parser-test
  (:use clojure.pprint
        clojure.test
        rssminer.util
        rssminer.test-common
        rssminer.parser)
  (:require [rssminer.db.feed :as db])
  (:import rssminer.Utils))

(deftest test-ppurl-rss
  (let [feed (parse-feed (slurp "test/ppurl-rss.xml"))]
    (is (= "ppurl" (:title feed)))
    (is (= "ppurl desc" (:description feed)))
    (is (= "http://www.ppurl.com" (:link feed)))
    (is (:published_ts feed))
    (is (= 1 (-> feed :entries count)))
    (is (= "computer" (-> feed :entries first :tags)))
    (are [k] (-> feed :entries first k)
         :author
         :title
         :summary
         :link
         :published_ts)))

(deftest test-most-len
  (is (<= 3 (count (most-len "abc" 3))))
  (is (<= 3 (count (most-len "abc" 6))))
  (is (<= 2 (count (most-len "abcdef" 2)))))

(deftest test-scottgu-atom
  (let [feed (parse-feed (slurp "test/scottgu-atom.xml"))]
    (is (= "ScottGu's Blog" (:title feed)))
    (is (=  "Scottgu's desc" (:description feed)))
    (is (= "http://weblogs.asp.net/scottgu/default.aspx" (:link feed)))
    (is (:published_ts feed))
    (is (= 1 (-> feed :entries count)))
    (is (= "ASP.NET;aCategory;Link Listing;MVC"
           (-> feed :entries first :tags)))
    (are [k] (-> feed :entries first k)
         :author
         :title
         :summary
         :link
         :updated_ts
         :published_ts)))

(def folder (java.io.File. "test/failed_rss/"))

(def fixture (join-fixtures [mysql-fixture lucene-fixture]))

(defn import-failed-rss []
  (fixture (fn [] (let [success (atom 1)]
                   (doseq [file (file-seq folder)]
                     (if (.isFile file)
                       (let [id (to-int (.getName file))
                             rss (Utils/trimRemoveBom (slurp file))
                             feeds (parse-feed rss)]
                         (when (and feeds (> (count (:entries feeds)) 1))
                           (println "ok" id)
                           (swap! success inc))
                         (db/save-feeds feeds id))))
                   (println "successed feeds" @success)))))
