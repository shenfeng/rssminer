(ns rssminer.search
  (:use [clojure.tools.logging :only [info]]
        [rssminer.util :only [ignore-error]]
        [rssminer.config :only [rssminer-conf]])
  (:import rssminer.search.Searcher))

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
    (reset! searcher (Searcher/initGlobalSearcher path @rssminer-conf))))

(defn index-feed [id rss-id {:keys [author tags title summary]}]
  (.index ^Searcher @searcher id rss-id author title summary tags))

(defn search* [term userid limit]
  {:body (.search ^Searcher @searcher term userid limit)
   :headers {"Cache-Control" "private, max-age=600"}})

(defn search-within-subs [term uid subids limit]
  {:body (.searchInSubIDs ^Searcher @searcher term uid subids limit)
   :headers {"Cache-Control" "private, max-age=600"}})
