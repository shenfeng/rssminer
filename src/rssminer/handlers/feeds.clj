(ns rssminer.handlers.feeds
  (:use (rssminer [util :only [user-id-from-session to-int assoc-if]]
                  [classify :only [on-feed-event]]))
  (:require [rssminer.db.user-feed :as uf]
            [rssminer.db.feed :as db]))

(defn user-vote [req]
  (let [fid (-> req :params :id to-int)
        vote (-> req :body :vote to-int)
        user-id (user-id-from-session req)]
    (uf/insert-user-vote user-id fid vote)
    (on-feed-event user-id fid)
    {:status 204 :body nil}))

(defn mark-as-read [req]
  (let [fid (-> req :params :id to-int)
        user-id (user-id-from-session req)]
    (uf/mark-as-read user-id fid)
    (on-feed-event user-id fid)
    {:status 204 :body nil}))

(defn get-by-subscription [req]
  (let [{:keys [rss-id limit sort offset]} (:params req)
        userid (user-id-from-session req)
        rssid (to-int rss-id)
        limit (to-int limit)
        offset (to-int offset)
        data (case sort
               "newest" (uf/fetch-sub-newest userid rssid limit offset)
               "oldest" (uf/fetch-sub-oldest userid rssid limit offset)
               "recommand" (uf/fetch-sub-likest userid rssid limit offset))]
    (if data
      {:body data
       :headers {"Cache-Control" "private, max-age=3600"} }
      data))) ;; cache one hour

