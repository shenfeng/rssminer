(ns rssminer.search
  (:use [clojure.tools.logging :only [info]]
        [rssminer.util :only [extract-text to-int]]
        [rssminer.db.util :only [with-h2 h2-query]])
  (:import [rssminer Searcher$Feed Searcher]
           java.sql.Clob))

(defonce indexer (atom nil))

(defn close-global-index-writer! []
  (when-not (nil? @indexer)
    (.close ^Searcher @indexer)
    (reset! indexer nil)))

(defn use-index-writer! [path]
  "It will close previous indexer"
  (close-global-index-writer!)
  (let [path (if (= path :RAM) "RAM" path)]
    (info "use index path" path)
    (reset! indexer (Searcher. path))))

(defn fetch-feeds [meta]
  (let [feeds (h2-query ["SELECT id, author, title, summary, link
              FROM TABLE(x int=?) T INNER JOIN feeds ON T.x = feeds.id"
                         (to-array (keys meta))])]
    (map #(let [^Searcher$Feed f (get meta (-> % :id str))]
            (assoc %
              :docid (.docId f)
              :tags (.tags f))) feeds)))

(defn index-feed
  [{:keys [id author title summary]} tags]
  (.index ^Searcher @indexer id author title (extract-text summary) tags))

(defn search* [term limit]
  (let [meta (.search ^Searcher @indexer term limit)]
    (fetch-feeds meta)))

(defn more-lik-this [req]
  (let [{:keys [feed-id limit] :or {limit 10}} (-> req :params)
        meta (.likeThis ^Searcher @indexer
                        (to-int feed-id) (to-int limit))]
    (fetch-feeds meta)))
