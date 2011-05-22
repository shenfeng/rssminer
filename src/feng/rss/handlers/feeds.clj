(ns feng.rss.handlers.feeds
  (:use (feng.rss [middleware :only [*user* *json-body*]]
                  [util :only [http-get]]
                  [parser :only [parse]])
        clojure.contrib.trace)  
  (:require [feng.rss.db.feed :as db]))

(defn- add-exists-fs-to-user [fs user-id]
  (if (db/fetch-user-feedsource  {:user_id user-id 
                                  :feedsource_id (:id fs)})
    {:status 400                        ;readd is not allowed
     :message "already added"}
    ;; add to user
    (let [_ (db/insert-user-feedsource
                   {:user_id user-id
                    :feedsource_id (:id fs)})
          feeds (db/fetch-feed (:id fs))
          ufs (doall (map #(db/insert-user-feed
                            {:user_id user-id
                             :feed_id (:id %)}) feeds))]
      {:items feeds
       :title (:title fs)
       :description (:description fs)
       :id (:id fs)})))

(defn- create-fs-add-it-to-user [link user-id]
  (if-let [feeds (parse (:body (http-get link)))]
    (let [ ;; 1. save feedsource
          fs (db/inser-feedsource
              {:link link 
               :description (:description feeds)
               :title (:title feeds)})
          ;; 2. assoc feedsource with user
          _ (db/insert-user-feedsource
                   {:user_id user-id
                    :feedsource_id (:id fs)})
          ;; 3. save feeds
          saved-feeds (map #(db/insert-feed
                             {:feedsource_id (:id fs)
                              :pub_date (:publishedDate %)
                              :link (:link %)
                              :description (-> % :description :value)
                              :title (:title %)
                              :author (:author %)
                              :guid (:uri %)}) (:entries feeds))
          ;; 4. assoc feeds with user
          ufs (doall (map #(db/insert-user-feed
                            {:user_id user-id
                             :feed_id (:id %)}) saved-feeds))]
      ;; 5. return data
      {:items saved-feeds
       :title (:title fs)
       :description (:description fs)
       :id (:id fs)})
    ;; not feed link
    {:status 400
     :message "bad feedlink"}))

(defn add-feedsource [req]
  (let [link (:link *json-body*)
        user-id (:id *user*)
        fs (db/fetch-feedsource {:link link})]
    (if fs                              
      (add-exists-fs-to-user fs user-id) ;we have the link
      ;; first time feedsource
      (create-fs-add-it-to-user link user-id))))

(defn get-feeds [req]
  (let [{:keys [fs-id limit offset] :or {limit 20 offset 0}} (:params req)
        fs (db/fetch-feedsource {:id (Integer. fs-id)})
        user-id (:id *user*)
        feeds (db/fetch-feed user-id (:id fs) limit offset)]
    {:items feeds
     :title (:title fs)
     :description (:description fs)
     :id fs-id}))
