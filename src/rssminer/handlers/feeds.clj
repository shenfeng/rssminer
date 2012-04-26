(ns rssminer.handlers.feeds
  (:use (rssminer [util :only [session-get to-int assoc-if get-expire]]))
  (:require [rssminer.db.user-feed :as uf]
            [rssminer.db.feed :as db]))

(defn user-vote [req]
  (let [fid (-> req :params :feed-id to-int)
        vote (-> req :body :vote to-int)
        user (session-get req :user)]
    (uf/insert-user-vote (:id user) fid vote)
    (if (-> user :conf :updated)
      {:status 204 :body nil}
      {:status 204 :body nil
       :session {:user (assoc user :conf
                              (assoc (:conf user) :updated true))}})))

(defn mark-as-read [req]
  (let [fid (-> req :params :feed-id to-int)
        user-id (:id (session-get req :user))]
    (uf/mark-as-read user-id fid)))

(defn get-by-subscription [req]
  (let [{:keys [rss-id limit offset] :or {limit 40 offset 0}} (:params req)
        uid (:id (session-get req :user))]
    (db/fetch-by-rssid uid (to-int rss-id) (to-int limit)
                       (to-int offset))))
