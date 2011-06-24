(ns freader.handlers.feeds
  (:use (freader [middleware :only [*user* *json-body*]]
                 [util :only [download-favicon download-feed-source]]
                 [parser :only [parse]]))
  (:require [freader.db.feed :as db]))

(defn- add-subscription-ret [user-id subscription-id]
  (let [f-c (db/fetch-favicon-count subscription-id)]
    (assoc (db/fetch-feeds-by-subscription-id user-id subscription-id)
      :favicon (:favicon f-c)
      :total_count (:count f-c)
      :unread_count (:count f-c))))

(defn- add-exists-subscription [subscription user-id]
  (let [uf (db/fetch-user-subscription
            {:user_id user-id
             :subscription_id (:id subscription)})]
    (if (empty? uf)
      (do (db/insert :user_subscription
                     {:user_id user-id
                      :title (:title subscription)
                      :subscription_id (:id subscription)})
          (add-subscription-ret user-id (:id subscription)))
      {:status 409                      ;readd is not allowed
       :message "already subscribed"})))

(defn- create-subscripton [link user-id]
  (if-let [feeds (parse (:body (download-feed-source link)))]
    (let [favicon (download-favicon link)
          ;; 1. save feedsource
          subscription (db/insert :subscriptions
                                  {:link link
                                   :user_id user-id
                                   :favicon favicon
                                   :description (:description feeds)
                                   :title (:title feeds)})]
      ;; 2. assoc feedsource with user
      (db/insert :user_subscription {:user_id user-id
                                     :title (:title subscription)
                                     :subscription_id (:id subscription)})
      (db/save-feeds subscription feeds user-id) ;; 3. save feeds
      ;; 5. return data
      (add-subscription-ret user-id (:id subscription)))
    ;; fetch feed error
    {:status 460
     :message "bad feedlink"}))

(defn add-subscription
  ([req]
     (let [link (:link *json-body*)
           user-id (:id *user*)]
       (add-subscription link user-id)))
  ([link user-id]
     (let [sub (db/fetch-subscription {:link link})]
       (if sub
         (add-exists-subscription sub user-id) ;we have the subscription
         ;; first time subscription
         (create-subscripton link user-id)))))

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
