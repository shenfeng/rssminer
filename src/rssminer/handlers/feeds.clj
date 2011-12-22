(ns rssminer.handlers.feeds
  (:use (rssminer [util :only [session-get to-int]]
                  [config :only [rssminer-conf]]
                  [http :only [client parse-response]]))
  (:require [rssminer.db.feed :as db])
  (:import rssminer.Utils))

(defn save-pref [req]
  (let [{:keys [feed-id pref]} (:params req)
        user-id (:id (session-get req :user))]
    (db/insert-pref user-id feed-id
                    (Boolean/parseBoolean pref))))

(defn get-by-subscription [req]
  (let [{:keys [rss-id limit offset] :or {limit 30 offset 0}} (:params req)]
    (db/fetch-by-rssid (:id (session-get req :user))
                       (to-int rss-id)
                       (to-int limit)
                       (to-int offset))))

(defn get-by-id [req]
  (let [feed-id (-> req :params :feed-id)]
    (db/fetch-by-id feed-id)))

(defn- rewrite-html [original link proxy]
  (if proxy
    (Utils/rewrite original link (:proxy-server @rssminer-conf))
    (Utils/rewrite original link)))

(defn- fetch-and-store-orginal [id link proxy]
  (let [resp (-> client (.execGet link) .get parse-response)]
    (if (= 200 (:status resp))
      (let [body (:body resp)]
        (db/save-feed-original id body)
        (rewrite-html body link proxy))
      {:status 404})))

(defn get-orginal [req]
  (let [{:keys [id p]} (-> req :params)
        {:keys [original link]} (db/fetch-orginal id)] ; proxy
    (if original
      (rewrite-html original link p)
      (fetch-and-store-orginal id link p))))

