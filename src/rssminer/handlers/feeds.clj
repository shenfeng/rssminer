(ns rssminer.handlers.feeds
  (:use (rssminer [util :only [user-id-from-session to-int assoc-if]]
                  [classify :only [on-feed-event]]
                  [config :only [cache-control]]))
  (:require [rssminer.db.feed :as db]
            [clojure.string :as str]))

(defn user-vote [req]
  (let [fid (-> req :params :id to-int)
        vote (-> req :body :vote to-int)
        user-id (user-id-from-session req)]
    (db/insert-user-vote user-id fid vote)
    (on-feed-event user-id fid)
    {:status 204 :body nil}))

(defn- mark-read [fid user-id]
  (db/mark-as-read user-id fid)
  (on-feed-event user-id fid))

(defn mark-as-read [req]
  (let [fid (-> req :params :id to-int)
        user-id (user-id-from-session req)]
    (mark-read fid user-id)
    {:status 204 :body nil}))

(defn get-feed [req]
  (let [fid (-> req :params :id to-int)
        user-id (user-id-from-session req)
        feed (db/fetch-feed user-id fid)]
    (when (= "1" (-> req :params :read))
      (mark-read fid user-id))
    {:body feed :headers cache-control}))

(defn get-by-subscription [req]
  (let [{:keys [rid limit sort offset]} (:params req)
        uid (user-id-from-session req)
        limit (to-int limit)
        offset (to-int offset)
        data (if (= -1 (.indexOf ^String rid (int \-)))
               (let [rssid (to-int rid)]
                 (case sort
                   "newest" (db/fetch-sub-newest uid rssid limit offset)
                   "oldest" (db/fetch-sub-oldest uid rssid limit offset)
                   "recommend" (db/fetch-sub-likest uid rssid limit offset)
                   "read" (db/fetch-sub-read uid rssid limit offset)
                   "voted" (db/fetch-sub-vote uid rssid limit offset)))
               (let [ids (map to-int (str/split rid #"-"))]
                 (case sort
                   "newest" (db/fetch-folder-newest uid ids limit offset)
                   "oldest" (db/fetch-folder-oldest uid ids limit offset)
                   "recommend" (db/fetch-folder-likest uid ids limit offset)
                   "read" (db/fetch-folder-read uid ids limit offset)
                   "voted" (db/fetch-folder-vote uid ids limit offset))))]
    (if (and (seq data) (not= "read" sort) (not= "voted" sort))
      {:body data :headers cache-control }
      data))) ;; cache one hour

