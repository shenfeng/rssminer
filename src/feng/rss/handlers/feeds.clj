(ns feng.rss.handlers.feeds
  (:use (feng.rss [middleware :only [*user* *json-body*]]
                  [util :only [http-get]]
                  [parser :only [parse]]))  
  (:require [feng.rss.db.feed :as db]))

(defn- add-exists-fs-to-user [fs user-id]
  (let [uf (db/fetch-user-feedsource
            {:user_id user-id 
             :feedsource_id (:id fs)})]
    (if (empty? uf)
      (let [_ (db/insert-user-feedsource
               {:user_id user-id
                :feedsource_id (:id fs)})
            feeds (db/fetch-feeds (:id fs))
            ufs (doall (map #(db/insert-user-feed
                              {:user_id user-id
                               :feed_id (:id %)}) feeds))]
        {:items feeds
         :title (:title fs)
         :description (:description fs)
         :id (:id fs)})
      {:status 400                      ;readd is not allowed
       :message "already added"})))

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

(defn- fetch-feeds-by-id [user-id fs-id limit offset]
  (let [fs (db/fetch-feedsource {:id (Integer. fs-id)})
        feeds (db/fetch-feeds user-id (:id fs) limit offset)]
    {:items feeds
     :title (:title fs)
     :description (:description fs)
     :id fs-id}))

(defn list-feeds [req]
  (let [{:keys [fs-id limit offset]
         :or {limit 20 offset 0}} (:params req)
        user-id (:id *user*)]
    (fetch-feeds-by-id user-id fs-id limit offset)))

(defn list-all-feeds [req]
  (let [user-id (:id *user*)
        ufs (db/fetch-user-feedsource {:user_id user-id})]
    (map #(fetch-feeds-by-id
           user-id (:feedsource_id %) 20 0) ufs)))
