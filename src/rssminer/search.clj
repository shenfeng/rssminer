(ns rssminer.search
  (:use [clojure.tools.logging :only [info]]
        [rssminer.util :only [to-int]]
        [rssminer.db.util :only [with-h2 h2-query]])
  (:import [rssminer Searcher$Feed Searcher]
           rssminer.classfier.NaiveBayes
           java.sql.Clob))

(defonce searcher (atom nil))

(defn close-global-index-writer! []
  (when-not (nil? @searcher)
    (.close ^Searcher @searcher)
    (reset! searcher nil)))

(defn use-index-writer! [path]
  "It will close previous searcher"
  (close-global-index-writer!)
  (let [path (if (= path :RAM) "RAM" path)]
    (info "use index path" path)
    (reset! searcher (Searcher. path))))

;;; meta Map<feed-id, {docid, feed-id}>
(defn fetch-feeds [meta user-id]
  (let [ids (to-array (keys meta))
        sql (if user-id
              ["SELECT id, author, title, summary, link, p.pref, tags
                FROM TABLE(x int=?) T INNER JOIN feeds f ON T.x = f.id
                LEFT JOIN user_feed_pref p
                     on p.feed_id = id and p.user_id = ?" ids user-id]
              ["SELECT id, author, title, summary, link, tags
                FROM TABLE(x int=?) T INNER JOIN feeds f ON T.x = f.id" ids])]
    (map #(let [^Searcher$Feed f (get meta (-> % :id str))]
            (assoc %
              :docid (.docId f))) (h2-query sql :convert))))

(defn index-feed
  [id {:keys [author tags title summary]}]
  (.index ^Searcher @searcher id author title summary tags))

(defn search* [term limit & {:keys [user-id]}]
  (let [meta (.search ^Searcher @searcher term limit)]
    (fetch-feeds meta user-id)))
