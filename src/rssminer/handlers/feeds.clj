(ns rssminer.handlers.feeds
  (:use (rssminer [util :only [to-int defhandler]]
                  [classify :only [on-feed-event]]
                  [config :only [cache-control]]))
  (:require [rssminer.db.feed :as db]
            [clojure.string :as str]))

(defhandler user-vote [req fid uid]
  (let [vote (-> req :body :vote to-int)
        fid (to-int fid)]
    (when (db/insert-user-vote uid fid vote)
      (on-feed-event uid fid))
    {:status 204 :body nil}))

(defn- mark-read [fid uid]
  (when (db/mark-as-read uid (to-int fid))
    (on-feed-event uid (to-int fid))))

(defhandler mark-as-read [req fid uid]
  (mark-read (to-int fid) uid)
  {:status 204 :body nil})

(defhandler get-feeds [req fid uid mr]
  ;; fid may be a list of ids
  (let [fids (map to-int (str/split fid #"-"))]
    (when (and (= 1 (count fids)) mr)   ;
      (mark-read (first fids) uid))
    {:body (db/fetch-feeds uid fids) :headers cache-control}))

(defhandler save-reading-time [req uid]
  (db/update-reading-time uid (:body req))
  {:status 204 :body nil})

(defhandler get-by-subscription [req rid limit sort offset uid]
  (let [data (if (= -1 (.indexOf ^String rid (int \-)))
               (let [rssid (to-int rid)]
                 (case sort
                   "newest" (db/fetch-sub-newest uid rssid limit offset)
                   "oldest" (db/fetch-sub-oldest uid rssid limit offset)
                   ;; "recommend" (db/fetch-sub-likest uid rssid limit offset)
                   "read" (db/fetch-sub-read uid rssid limit offset)
                   "voted" (db/fetch-sub-vote uid rssid limit offset)))
               (let [ids (map to-int (str/split rid #"-"))]
                 (case sort
                   "newest" (db/fetch-folder-newest uid ids limit offset)
                   "oldest" (db/fetch-folder-oldest uid ids limit offset)
                   ;; "recommend" (db/fetch-folder-likest uid ids limit offset)
                   "read" (db/fetch-folder-read uid ids limit offset)
                   "voted" (db/fetch-folder-vote uid ids limit offset))))]
    (if (and (seq data) (not= "read" sort) (not= "voted" sort))
      {:body data :headers cache-control }
      data)))
