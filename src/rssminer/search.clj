(ns rssminer.search
  (:use [clojure.tools.logging :only [info]]
        [rssminer.util :only [to-int ignore-error]]
        [rssminer.db.util :only [mysql-query]])
  (:import rssminer.Searcher)
  (:require [clojure.string :as str]))

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
    (reset! searcher (Searcher/initGlobalSearcher path))))

(defn index-feed [id rss-id {:keys [author tags title summary]}]
  (.index ^Searcher @searcher id rss-id author title summary tags))

;;; Cyclic load dependency
(defn fetch-feeds [feed-ids user-id]
  (mysql-query
   [(str "SELECT id, author, link, title, tags, published_ts, f.rss_link_id,
          uf.read_date, uf.vote_user, uf.vote_sys FROM feeds f
          LEFT JOIN user_feed uf on user_id = ? and id = uf.feed_id
      WHERE f.id in " "(" (str/join ", " feed-ids) ")")
    user-id]))

;; (defn update-index [id html]
;;   (try
;;     ;; TODO replace 1 with real rss-id
;;     (.updateIndex ^Searcher @searcher (to-int id) 1 html)
;;     (catch Exception e
;;       (println "updating index " id "error!!"))))

(defn search* [term user-id rss-ids limit]
  (let [meta (.search ^Searcher @searcher term (map to-int rss-ids) limit)]
    {:body (if (seq meta)
             (fetch-feeds meta user-id)
             [])
     :headers {"Cache-Control" "private, max-age=3600"}} ))
