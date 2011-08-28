(ns rssminer.search
  (:use [clojure.tools.logging :only [info]])
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
    (info "use index path" path)
    (reset! indexer (Searcher. path))))

(defn commit []
  (.commit ^Searcher @indexer))

(defn index-feed [{:keys [id rss_link_id author title summary] :as feed}]
  (.index ^Searcher @indexer id rss_link_id author title summary))

(defn search [req]
  (let [{:keys [term limit] :or {limit "10"}} (-> req :params)]
    (.searchForTitle ^Searcher @indexer
                     term (Integer/parseInt limit))))
