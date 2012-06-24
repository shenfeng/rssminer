(ns rssminer.handlers.subscriptions
  (:use (rssminer [redis :only [fetcher-enqueue]]
                  [util :only [to-int user-id-from-session time-since]])
        [rssminer.database :only [mysql-insert-and-return]]
        [clojure.tools.logging :only [info]])
  (:require [rssminer.db.subscription :as db]
            [rssminer.db.feed :as fdb]
            [clojure.string :as str]))

(def ^{:private true} enqueue-keys [:id :url :check_interval :last_modified])

(defn subscribe [url user-id title group-name]
  (let [sub (or (db/fetch-rss-link {:url url})
                (mysql-insert-and-return :rss_links {:url url
                                                     :user_id user-id}))]
    (fetcher-enqueue (select-keys sub enqueue-keys))
    (if-let [us (db/fetch-subscription user-id (:id sub))]
      us
      (mysql-insert-and-return :user_subscription
                               {:user_id user-id
                                :group_name group-name
                                :title title
                                :rss_link_id (:id sub)}))))

(defn polling-fetcher [req]             ;; wait for fetcher return
  (let [rss-id (-> req :params :rss-id to-int)]
    (db/fetch-user-sub rss-id)))

(defn list-subscriptions [req]
  (db/fetch-user-subs (user-id-from-session req))  )

(defn add-subscription [req]
  (let [{:keys [link g]}  (-> req :body)
        user-id (user-id-from-session req)]
    (info (str "user: " user-id " add subscription: " link))
    ;; enqueue, client need to poll for result
    (subscribe link user-id nil g)))

(defn save-sort-order [req]
  (let [uid (user-id-from-session req)
        ;; [{:g group :ids [id, id, id]}]
        data (mapcat (fn [{:keys [ids g]}]
                       (map (fn [id] {:g g :id id}) ids)) (:body req))]
    (db/update-sort-order uid data)
    {:status 204}))

(defn unsubscribe [req]
  (let [user-id (user-id-from-session req)
        rss-id (-> req :params :rss-id to-int)]
    (db/delete-subscription user-id rss-id)))
