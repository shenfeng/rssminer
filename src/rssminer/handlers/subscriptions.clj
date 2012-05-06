(ns rssminer.handlers.subscriptions
  (:use (rssminer [redis :only [fetcher-enqueue]]
                  [util :only [to-int session-get time-since]])
        [rssminer.db.util :only [mysql-insert-and-return]]
        [clojure.tools.logging :only [info]])
  (:require [rssminer.db.subscription :as db]
            [rssminer.db.feed :as fdb]))

(def ^{:private true} enqueue-keys [:id :url :check_interval :last_modified])

(defn- add-rss-link [user-id url]
  (let [sub (mysql-insert-and-return :rss_links {:url url
                                                 :user_id user-id})]
    (fetcher-enqueue (select-keys sub enqueue-keys))
    sub))

(defn subscribe [url user-id title group-name]
  (let [sub (or (db/fetch-rss-link {:url url})
                (add-rss-link user-id url))]
    (if-let [us (db/fetch-subscription user-id (:id sub))]
      us
      (mysql-insert-and-return :user_subscription
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

(defn list-subscriptions [req]
  (let [user (session-get req :user)]
    (if (-> req :params :only_url)
      (map :url (db/fetch-user-subsurls (:id user))) ; for extension
      (let [like (or (-> user :conf :like_score) 1)
            neutral (or (-> user :conf :neutral_score) 0)]
        (db/fetch-user-subs (:id user) like neutral)))))

(defn add-subscription [req]
  (let [link  (-> req :body :link)
        user-id (:id (session-get req :user))]
    (info (str "user: " user-id " add subscription: " link))
    ;; enqueue, client need to poll for result
    (subscribe link user-id nil nil)))

(defn save-sort-order [req]
  (db/update-sort-order (:id (session-get req :user)) (:body req))
  {:status 204})

(defn unsubscribe [req]
  (let [user-id (:id (session-get req :user))
        rss-id (-> req :params :rss-id to-int)]
    (db/delete-subscription user-id rss-id)))
