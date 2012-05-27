(ns rssminer.handlers.feeds
  (:use (rssminer [util :only [user-id-from-session to-int
                               assoc-if get-expire]]
                  [classify :only [on-user-vote]]))
  (:require [rssminer.db.user-feed :as uf]
            [rssminer.db.feed :as db]))

(defn user-vote [req]
  (let [fid (-> req :params :id to-int)
        vote (-> req :body :vote to-int)
        user-id (user-id-from-session req)]
    (uf/insert-user-vote user-id fid vote)
    (on-user-vote user-id fid (= vote 1))
    {:status 204 :body nil}))

(defn mark-as-read [req]
  (let [fid (-> req :params :id to-int)
        user-id (user-id-from-session req)]
    (uf/mark-as-read user-id fid)
    {:status 204 :body nil}))

(defn get-by-subscription [req]
  (let [{:keys [rss-id limit sort offset]} (:params req)
        data (db/fetch-by-rssid (user-id-from-session req)
                                (to-int rss-id)
                                (min 40 (to-int limit))
                                (to-int offset)
                                sort)]
    (if data
      {:body data
       :headers {"Cache-Control" "private, max-age=3600"} }
      data))) ;; cache one hour

