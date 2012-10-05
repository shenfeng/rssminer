(ns rssminer.search
  (:use [clojure.tools.logging :only [info]]
        [rssminer.util :only [ignore-error]]
        [rssminer.config :only [rssminer-conf cache-control]])
  (:import rssminer.search.Searcher))

(defonce searcher (atom nil))

(defn close-global-index-writer! [& {:keys [optimize]}]
  (when-not (nil? @searcher)
    (.close ^Searcher @searcher (= optimize true))
    (reset! searcher nil)))

(defn use-index-writer! [path]
  (close-global-index-writer!)          ; close previous searcher
  (let [path (if (= path :RAM) "RAM" path)]
    (info "use index path" path)
    (reset! searcher (Searcher/initGlobalSearcher path @rssminer-conf))))

(defn index-feed [id rss-id {:keys [author tags title summary]}]
  (.index ^Searcher @searcher id rss-id author title summary tags))

(defn search* [term tags authors userid limit fs]
  {:body (.search ^Searcher @searcher term tags authors userid limit (nil? fs))
   :headers cache-control})

;; (defn search-within-subs [term uid subids limit]
;;   {:body (.searchInSubIDs ^Searcher @searcher term uid subids limit)
;;    :headers cache-control})
