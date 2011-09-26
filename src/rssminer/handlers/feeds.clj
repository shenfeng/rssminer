(ns rssminer.handlers.feeds
  (:use [rssminer.util :only [session-get]])
  (:require [rssminer.db.feed :as db]))

(defn save-pref [req]
  (let [{:keys [feed-id pref]} (:params req)
        user-id (:id (session-get req :user))]
    (db/insert-pref user-id feed-id
                    (Boolean/parseBoolean pref))))

