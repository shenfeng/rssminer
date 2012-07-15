(ns rssminer.handlers.feeds
  (:use (rssminer [util :only [user-id-from-session to-int assoc-if]]
                  [classify :only [on-feed-event]]
                  [config :only [cache-control]]))
  (:require [rssminer.db.user-feed :as uf]
            [rssminer.db.feed :as db]
            [clojure.string :as str]))

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
  (let [{:keys [rid limit sort offset]} (:params req)
        uid (user-id-from-session req)
        limit (to-int limit)
        offset (to-int offset)
        data (if (= -1 (.indexOf ^String rid (int \-)))
               (let [rssid (to-int rid)]
                 (case sort
                   "newest" (uf/fetch-sub-newest uid rssid limit offset)
                   "oldest" (uf/fetch-sub-oldest uid rssid limit offset)
                   "recommend" (uf/fetch-sub-likest uid rssid limit offset)
                   "read" (uf/fetch-sub-read uid rssid limit offset)
                   "voted" (uf/fetch-sub-vote uid rssid limit offset)))
               (let [ids (map to-int (str/split rid #"-"))]
                 (case sort
                   "newest" (uf/fetch-folder-newest uid ids limit offset)
                   "oldest" (uf/fetch-folder-oldest uid ids limit offset)
                   "recommend" (uf/fetch-folder-likest uid ids limit offset)
                   "read" (uf/fetch-folder-read uid ids limit offset)
                   "voted" (uf/fetch-folder-vote uid ids limit offset))))]
    (if (and (seq data) (not= "read" sort) (not= "voted" sort))
      {:body data
       :headers cache-control }
      data))) ;; cache one hour

