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
      (let [_ (db/insert-user-subscription
               {:user_id user-id
                :subscription_id (:id subscription)})]
        subscription)
      {:status 409                      ;readd is not allowed
       :message "already subscribed"})))

(defn- create-subscripton [link user-id]
  (if-let [feeds (parse (:body (http-get link)))]
    (let [favicon (get-favicon link)
          ;; 1. save feedsource
          subscription (db/insert-subscription
                        {:link link
                         :user_id user-id
                         :favicon favicon
                         :description (:description feeds)
                         :title (:title feeds)})
          ;; 2. assoc feedsource with user
          _ (db/insert-user-subscription
             {:user_id user-id
              :subscription_id (:id subscription)})
          ;; 3. save feeds
          saved-feeds (doall
                       (map #(db/insert-feed
                              {:subscription_id (:id subscription)
                               :author (:author %)
                               :title (:title %)
                               :summary (-> % :description :value)
                               :alternate (:link %)
                               :published_ts (:publishedDate %)})
                            (:entries feeds)))]
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
