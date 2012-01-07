(ns rssminer.search
  (:use [clojure.tools.logging :only [info]]
        [rssminer.util :only [to-int ignore-error]]
        [rssminer.db.util :only [with-h2 h2-query]])
  (:import rssminer.Searcher))

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

(defn fetch-feeds [feed-ids user-id]
  (let [sql (if user-id
              ["SELECT id, author, title, link, p.pref, tags
                FROM TABLE(x int=?) T INNER JOIN feeds f ON T.x = f.id
                LEFT JOIN user_feed_pref p
                     on p.feed_id = id and p.user_id = ?" feed-ids user-id]
              ["SELECT id, author, title, link, tags
                FROM TABLE(x int=?) T INNER JOIN feeds f ON T.x = f.id"
               feed-ids])]
    (h2-query sql :convert)))

(defn index-feed [id {:keys [author tags title summary]}]
  (ignore-error
   (.index ^Searcher @searcher id author title summary tags)))

(defn update-index [id html]
  (try
    (.updateIndex ^Searcher @searcher (to-int id) html)
    (catch Exception e
      (println "updating index " id "error!!"))))

(defn search* [term limit & {:keys [user-id]}]
  (let [meta (.search ^Searcher @searcher term limit)]
    (fetch-feeds meta user-id)))
