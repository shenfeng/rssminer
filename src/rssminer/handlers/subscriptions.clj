(ns rssminer.handlers.subscriptions
  (:use (rssminer [redis :only [fetcher-enqueue]]
                  [util :only [to-int session-get time-since]])
        [rssminer.db.util :only [h2-insert-and-return]]
        [clojure.tools.logging :only [info]])
  (:require [rssminer.db.subscription :as db]
            [rssminer.db.feed :as fdb]))

(def ^{:private true} enqueue-keys [:id :url :check_interval :last_modified])

(defn subscribe [url user-id title group-name]
  (let [sub (or (db/fetch-rss-link {:url url})
                (h2-insert-and-return :rss_links {:url url
                                                  :user_id user-id}))]
    (fetcher-enqueue (select-keys sub enqueue-keys))
    (if-let [us (db/fetch-subscription {:user_id user-id
                                        :rss_link_id (:id sub)})]
      us
      (h2-insert-and-return :user_subscription
                            {:user_id user-id
                             :group_name group-name
                             :title title
                             :rss_link_id (:id sub)}))))

(defn polling-subscription [req]
  (let [rss-id (-> req :params :rss-id to-int)
        user (session-get req :user)]
    (db/fetch-user-sub rss-id (:id user)
                       (time-since user)
                       (or (-> user :conf :like_score) 1.0)
                       (or (-> user :conf :neutral_score) 0))))

(defn add-subscription [req]
  (let [link  (-> req :body :link)
        user-id (:id (session-get req :user))]
    (info "user " user-id " add subscription: " link)
    ;; enqueue, client need to poll for result
    (subscribe link user-id nil nil)))

(defn customize-subscription [req]
  (let [user-id (:id (session-get req :user))]
    (db/update-subscription user-id (-> req :params :id to-int) (:body req))))

(defn unsubscribe [req]
  (let [user-id (:id (session-get req :user))]
    (db/delete-subscription user-id (-> req :params :id to-int))))
