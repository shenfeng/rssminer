(ns rssminer.search
  (:import rssminer.Searcher))

(defonce indexer (atom nil))

(defn close-global-index-writer! []
  (when-not (nil? @indexer)
    (.close ^Searcher @indexer)
    (reset! indexer nil)))

(defn use-index-writer! [path]
  "It will close previous indexer"
  (close-global-index-writer!)
  (let [path (if (= path :RAM) "RAM" path)]
    (reset! indexer (Searcher. path))))

(defn index-feed [{:keys [id rss_link_id author title summary] :as feed}]
  (.index ^Searcher @indexer id rss_link_id author title summary))

(defn search [req]
  (.searchForTitle ^Searcher @indexer
                   (-> req :params :term) 10))
