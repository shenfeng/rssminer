(ns rssminer.search
  (:use [clojure.tools.logging :only [info debug]]
        [rssminer.util :only [extract-text to-int]]
        [rssminer.config :only [in-dev?]])
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
    (debug "use index path" path)
    (reset! indexer (Searcher. path (in-dev?)))))

(defn commit []
  (.commit ^Searcher @indexer))

(defn index-feed
  [{:keys [id rss_link_id author title summary] :as feed} categories]
  (.index ^Searcher @indexer id rss_link_id author title
          (extract-text summary) categories))

(defn search* [term limit]
  (map #(dissoc (bean %) :class)
       (.search ^Searcher @indexer term limit)))

(defn search [req]
  )

(defn more-lik-this [req]
  (let [{:keys [id limit] :or {limit 10}} (-> req :params)]
    (map #(dissoc (bean %) :class)
         (.likeThis ^Searcher @indexer
                    (to-int id) (to-int limit)))))

(defn search-ac-title [req]
  (let [{:keys [term limit] :or {limit "10"}} (-> req :params)]
    (.searchForTitle ^Searcher @indexer
                     term (Integer/parseInt limit))))
