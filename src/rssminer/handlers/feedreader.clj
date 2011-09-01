(ns rssminer.handlers.feedreader
  (:use [rssminer.handlers.subscriptions :only [get-overview*]]
        [rssminer.util :only [session-get to-int]]
        [rssminer.search :only [search*]]
        [clojure.string :only [blank?]]
        [rssminer.db.feed :only [fetch-latest-feed]])
  (:require [rssminer.views.feedreader :as view]
            [rssminer.config :as cfg]))

(defn landing-page [req]
  (view/landing-page))

(defn index-page [req]
  (view/index-page {:overview (get-overview* (:id (session-get req :user)))}))

(defn dashboard-page [req]
  (view/dashboard-page))

(defn browse-feed [req]
  (let [{:keys [term limit] :or {limit 20}} (:params req)]
    (if (blank? term)
      (view/browse-feed {:feeds (flatten (map #(search* (str "tag:" %) 2)
                                              cfg/popular-tags))
                         :tags cfg/popular-tags})
      (view/browse-feed {:feeds (search* term (to-int limit))
                         :tags cfg/popular-tags}))))
