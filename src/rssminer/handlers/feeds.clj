(ns rssminer.handlers.feeds
  (:use [rssminer.util :only [session-get to-int]])
  (:require [rssminer.db.feed :as db]))

(defn save-pref [req]
  (let [{:keys [feed-id pref]} (:params req)
        user-id (:id (session-get req :user))]
    (db/insert-pref user-id feed-id
                    (Boolean/parseBoolean pref))))

(defn get-by-tag [req]
  (let [{:keys [tag limit offset] :or {limit 20 offset 0}} (:params req)]
    (db/fetch-by-tag (:id (session-get req :user)) tag
                     (to-int limit)
                     (to-int offset))))

(defn get-by-subscription [req]
  (let [{:keys [rss-id limit offset] :or {limit 7 offset 0}} (:params req)]
    (db/fetch-by-rssid (:id (session-get req :user))
                       (to-int rss-id)
                       (to-int limit)
                       (to-int offset))))

(defn get-by-id [req]
  (let [feed-id (-> req :params :feed-id)]
    (db/fetch-by-id (:id (session-get req :user)) feed-id)))
