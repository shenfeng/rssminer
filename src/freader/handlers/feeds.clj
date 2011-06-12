(ns freader.handlers.feeds
  (:use (freader [middleware :only [*user* *json-body*]]
                  [util :only [http-get get-favicon]]
                  [parser :only [parse]]))
  (:require [freader.db.feed :as db]))

(defn- fetch-feeds-by-id
  ([user-id subscription-id]
     (fetch-feeds-by-id user-id subscription-id 20 0))
  ([user-id subscription-id limit offset]
     (let [subscription (db/fetch-subscription {:id subscription-id})
           feeds (db/fetch-feeds (:id subscription) limit offset)
           items (map
                  (fn [f]
                    (assoc f
                      :comments (or
                                 (db/fetch-comments user-id (:id f)) [])
                      :categories (or
                                   (db/fetch-categories user-id (:id f)) [])))
                  feeds)]
       {:id subscription-id
        :title (:title subscription)
        :description (:description subscription)
        :alternate (:alternate subscription)
        :updated_ts (:updated_ts subscription)
        :continuation (when (= limit (count feeds)) (+ offset limit))
        :items items})))

(defn- save-feeds [subscription feeds user-id]
  (doseq [feed (:entries feeds)]
    (let [saved-feed
          (db/insert :feeds
                     {:subscription_id (:id subscription)
                      :author (:author feed)
                      :title (:title feed)
                      :summary (-> feed :description :value)
                      :alternate (:link feed)
                      :published_ts (:publishedDate feed)})
          categories (:categories feed)]
      (doseq [c categories]
        (db/insert :feedcategory
                   {:user_id user-id
                    :feed_id (:id saved-feed)
                    :type "tag"
                    :text (:name c)})))))

(defn- add-subscription-ret [user-id subscription-id]
  (let [f-c (db/fetch-favicon-count subscription-id)]
    (assoc (fetch-feeds-by-id user-id subscription-id)
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
                      :subscription_id (:id subscription)})
          (add-subscription-ret user-id (:id subscription)))
      {:status 409                      ;readd is not allowed
       :message "already subscribed"})))

(defn- create-subscripton [link user-id]
  (if-let [feeds (parse (:body (http-get link)))]
    (let [favicon (get-favicon link)
          ;; 1. save feedsource
          subscription (db/insert :subscriptions
                                  {:link link
                                   :user_id user-id
                                   :favicon favicon
                                   :description (:description feeds)
                                   :title (:title feeds)})]
      ;; 2. assoc feedsource with user
      (db/insert :user_subscription {:user_id user-id
                                     :subscription_id (:id subscription)})
      (save-feeds subscription feeds user-id) ;; 3. save feeds
      ;; 5. return data
      (add-subscription-ret user-id (:id subscription)))
    ;; fetch feed error
    {:status 460
     :message "bad feedlink"}))

(defn add-subscription [req]
  (let [link (:link *json-body*)
        user-id (:id *user*)
        subscription (db/fetch-subscription {:link link})]
    (if subscription
      (add-exists-subscription subscription user-id) ;we have the subscription
      ;; first time subscription
      (create-subscripton link user-id))))

(defn get-subscription [req]
  (let [{:keys [subscription-id limit offset]
         :or {limit 20 offset 0}} (:params req)
         subscription-id (Integer. subscription-id)
         limit (Integer. limit)
         offset (Integer. offset)
         user-id (:id *user*)]
    (fetch-feeds-by-id user-id subscription-id limit offset)))

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
