(ns feng.rss.handlers.feeds
  (:use (feng.rss [middleware :only [*user* *json-body*]]
                  [util :only [http-get]]
                  [parser :only [parse]])
        clojure.contrib.trace)  
  (:require [feng.rss.db.feed :as db]))

(defn add-feedsource [req]
  (let [link (:link *json-body*)
        user-id (:id *user*)
        resp (http-get link)
        feeds (parse (:body resp))]
    (if feeds
      ;; 1. save feedsource
      (let [fs (db/inser-feedsource {:link link
                                     :description (:description feeds)
                                     :title (:title feeds)})
            ;; 2. assoc feedsource with user
            user-fs (db/insert-user-feedsource {:user_id user-id
                                                :feedsource_id (:id fs)})
            ;; 3. save feeds
            saved-feeds (map #(db/insert-feed {:feedsource_id (:id fs)
                                      :pub_date (:publishedDate %)
                                      :link (:link %)
                                      :description (-> % :description :value)
                                      :title (:title %)
                                      :author (:author %)
                                      :guid (:uri %)}) (:entries feeds))
            ;; 4. assoc feeds with user
            ufs (doall
                 (map #(db/insert-user-feed {:user_id user-id
                                             :feed_id (:id %)}) saved-feeds))]
        ;; 5. return data
        {:items saved-feeds
         :title (:title fs)
         :description (:description fs)
         :id (:id fs)})
      {:status 400
       :message "bad feedlink"})))

(defn get-feeds [req]
  (let [{:keys [fs-id limit offset] :or {limit 10 offset 0}} (:params req)
        fs (db/fetch-feedsource-by-id (Integer. fs-id))
        user-id (:id *user*)
        feeds (db/fetch-feeds user-id (:id fs) limit offset)]
    {:items feeds
     :title (:title fs)
     :description (:description fs)
     :id fs-id}))
