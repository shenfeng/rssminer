(ns rssminer.handlers.feeds
  (:require [rssminer.db.feed :as db]))

(defn get-feed [req]
  (db/fetch-feed (-> req :params :feed-id)))

