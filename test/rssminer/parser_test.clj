(ns rssminer.parser-test
  (use clojure.pprint
       clojure.test
       rssminer.parser))

(deftest test-ppurl-rss
  (let [feed (parse-feed (slurp "test/ppurl-rss.xml"))]
    (is (= "ppurl" (:title feed)))
    (is (= "ppurl desc" (:description feed)))
    (is (= "http://www.ppurl.com" (:link feed)))
    (is (:published_ts feed))
    (is (= 1 (-> feed :entries count)))
    (is (= #{"r" "computer"} (-> feed :entries first :tags)))
    (are [k] (-> feed :entries first k)
         :author
         :title
         :summary
         :link
         :published_ts)))

(deftest test-scottgu-atom
  (let [feed (parse-feed (slurp "test/scottgu-atom.xml"))]
    (is (= "ScottGu's Blog" (:title feed)))
    (is (=  "Scottgu's desc" (:description feed)))
    (is (= "http://weblogs.asp.net/scottgu/default.aspx" (:link feed)))
    (is (:published_ts feed))
    (is (= 1 (-> feed :entries count)))
    (is (= #{"link listing" "asp.net" "mvc" "acategory"}
           (-> feed :entries first :tags)))
    (are [k] (-> feed :entries first k)
         :author
         :title
         :summary
         :link
         :updated_ts
         :published_ts)))
