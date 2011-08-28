(ns rssminer.handlers.feedreader
  (:use [rssminer.handlers.subscriptions :only [get-overview*]]
        [rssminer.util :only [session-get]]
        [rssminer.search :only [search*]]
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
  (view/browse-feed {:feeds (flatten (map #(search* (str "categories:" %) 2)
                                          cfg/popular-tags))
                     :tags cfg/popular-tags}))
