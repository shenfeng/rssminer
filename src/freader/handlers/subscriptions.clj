(ns freader.handlers.subscriptions
  (:use (freader [middleware :only [*user* *json-body*]]
                 [util :only [download-favicon download-feed-source]]
                 [parser :only [parse]]
                 [config :only [ungroup]]))
  (:require [freader.db.subscription :as db]))

(defn- add-subscription-ret [us subscription count]
  {:group_name (:group_name us)
   :id (:id subscription)
   :total_count count
   :unread_count count
   :title (:title us)
   :favicon (:favicon subscription)})

(defn- add-exists-subscription [subscription user-id
                                & {:keys [group-name title]}]
  (let [us (db/fetch-user-subscription
            {:user_id user-id
             :subscription_id (:id subscription)})]
    (if us
      {:status 409                      ;readd is not allowed
       :message "Already subscribed"}
      (let [us (db/insert :user_subscription
                          {:user_id user-id
                           :group_name (or group-name ungroup)
                           :title (or title (:title subscription))
                           :subscription_id (:id subscription)})
            count (db/fetch-feeds-count-by-id (:id subscription))]
        (add-subscription-ret us subscription count)))))

(defn- create-subscripton [link user-id & {:keys [group-name title]}]
  (if-let [feeds (parse (:body (download-feed-source link)))]
    (let [favicon (download-favicon link)
          ;; 1. save feedsource
          subscription (db/insert :subscriptions
                                  {:link link
                                   :user_id user-id
                                   :favicon favicon
                                   :description (:description feeds)
                                   :title (:title feeds)})
          ;; 2. assoc feedsource with user
          us (db/insert :user_subscription
                        {:user_id user-id
                         :group_name (or group-name ungroup)
                         :title (or title (:title subscription))
                         :subscription_id (:id subscription)})]
      (db/save-feeds subscription feeds user-id) ;; 3. save feeds
      ;; 5. return data
      (add-subscription-ret us subscription (count feeds)))
    ;; fetch feeds error
    {:status 460
     :message "Bad feedlink"}))

(defn add-subscription* [link user-id & options]
  (let [sub (db/fetch-subscription {:link link})]
    (if sub
      ;; we have the subscription
      (apply add-exists-subscription sub user-id options)
      ;; first time subscription
      (apply create-subscripton link user-id options))))

(defn add-subscription [req]
  (let [link (:link *json-body*)
        user-id (:id *user*)]
    (add-subscription* link user-id)))

(defn get-subscription [req]
  (let [{:keys [id limit offset]
         :or {limit 20 offset 0}} (:params req)
         id (Integer. id)
         limit (Integer. limit)
         offset (Integer. offset)
         user-id (:id *user*)]
    (db/fetch-feeds-by-subscription-id user-id id limit offset)))

(defn get-overview [req]
  (let [user-id (:id *user*)
        overview (db/fetch-overview user-id)
        map (reduce
             (fn [m item]
               (let [group-name (:group_name item)
                     c (dissoc (into {} (seq item)) :group_name)
                     items (m group-name)]
                 (assoc m group-name
                        (conj items c))))
             {} overview)]
    (for [[k v] map] {:group_name k
                      :subscriptions v})))

(defn customize-subscription [req]
  (let [user-id (:id *user*)
        subscription-id (-> req :params :id Integer.)]
    (assoc
        (db/update-user-subscription user-id subscription-id *json-body*)
      :id subscription-id)))

(defn unsubscribe [req]
  (let [user-id (:id *user*)
        subscription-id (-> req :params :id Integer.)]
    (db/delete-user-subscription user-id subscription-id)))
