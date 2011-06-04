(ns feng.rss.handlers.feeds
  (:use (feng.rss [middleware :only [*user* *json-body*]]
                  [util :only [http-get get-favicon]]
                  [parser :only [parse]]))  
  (:require [feng.rss.db.feed :as db]))

(defn- add-exists-subscription [subscription user-id]
  (let [uf (db/fetch-user-subscription
            {:user_id user-id 
             :subscription_id (:id subscription)})]
    (if (empty? uf)
      (do (db/insert :user_subscription
                     {:user_id user-id
                      :subscription_id (:id subscription)})
          subscription)
      {:status 409                      ;readd is not allowed
       :message "already subscribed"})))

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
      subscription)
    ;; fetch feed error
    {:status 460
     :message "bad feedlink"}))

(defn- fetch-feeds-by-id [user-id subscription-id limit offset]
  (let [subscription (db/fetch-subscription {:id subscription-id})
        feeds (db/fetch-feeds (:id subscription) limit offset)]
    ;; TODO return what spec says
    (assoc subscription
      :items feeds)))

(defn add-subscription [req]
  (let [link (:link *json-body*)
        user-id (:id *user*)
        subscription (db/fetch-subscription {:link link})]
    (if subscription                     
      (add-exists-subscription subscription user-id) ;we have the subscription
      ;; first time subscription
      (create-subscripton link user-id))))

(defn get-feeds-by-subscription-id [req]
  (let [{:keys [subscription-id limit offset]
         :or {limit 20 offset 0}} (:params req)
         subscription-id (Integer. subscription-id)
         user-id (:id *user*)]
    (fetch-feeds-by-id user-id subscription-id limit offset)))
