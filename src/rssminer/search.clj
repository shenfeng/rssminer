(ns rssminer.search
  (:use [clojure.tools.logging :only [info]]
        [rssminer.util :only [extract-text to-int]]
        [rssminer.db.util :only [with-h2 h2-query]]
        [clojure.java.jdbc :only [with-query-results]])
  (:import rssminer.Searcher
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

(defn toggle-infostream [toggle]
  (when-not (nil? @indexer)
    (.toggleInfoStream ^Searcher @indexer toggle)))

(defn index-feed
  [{:keys [id author title summary]} tags]
  (.index ^Searcher @indexer id author title
          (extract-text summary) tags))

(defn search* [term limit]
  (map #(dissoc (bean %) :class)
       (.search ^Searcher @indexer term limit)))

(defn more-lik-this [req]
  (let [{:keys [feed-id limit] :or {limit 10}} (-> req :params)]
    (map #(dissoc (bean %) :class)
         (.likeThis ^Searcher @indexer
                    (to-int feed-id) (to-int limit)))))

(defn search-ac-title [req]
  (let [{:keys [term limit] :or {limit "10"}} (-> req :params)]
    (.searchForTitle ^Searcher @indexer
                     term (Integer/parseInt limit))))

