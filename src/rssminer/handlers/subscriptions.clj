(ns rssminer.handlers.subscriptions
  (:use (rssminer [middleware :only [*user* *json-body*]]
                  [http :only [download-favicon download-rss]]
                  [parser :only [parse-feed]]
                  [util :only [to-int if-lets md5-sum]]
                  [config :only [ungroup]])
        [rssminer.db.util :only [h2-insert h2-insert-and-return]]
        [clojure.tools.logging :only [info]])
  (:require [rssminer.db.subscription :as db]
            [rssminer.db.feed :as fdb])
  (:import java.io.StringReader))

(defn- add-subscription-ret [subscription rss count]
  {:group_name (:group_name subscription)
   :id (:id subscription)
   :total_count count
   :unread_count count
   :title (:title subscription)
   :favicon (:favicon rss)})

(defn- add-exists-subscription [subscription user-id
                                & {:keys [group-name title]}]
  (if-let [us (db/fetch-subscription
               {:user_id user-id
                :rss_link_id (:id subscription)})]
    {:status 409                        ;readd is not allowed
     :message "Already subscribed"}
    (let [us (h2-insert-and-return :user_subscription
                                   {:user_id user-id
                                    :group_name (or group-name ungroup)
                                    :title (or title (:title subscription))
                                    :rss_link_id (:id subscription)})
          count (db/fetch-feeds-count-by-id (:id subscription))]
      (add-subscription-ret us subscription count))))

(defn- create-subscripton [link user-id & {:keys [group-name title]}]
  (if-lets [{:keys [status headers body]} (download-rss link)
            feeds (parse-feed body)]
           (let [rss (h2-insert-and-return
                      :rss_links
                      {:url link
                       :last_modified (:last-modified headers)
                       :last_md5 (md5-sum body)
                       :user_id user-id
                       :favicon (download-favicon link)
                       :description (:description feeds)
                       :title (:title feeds)})
                 us (h2-insert-and-return :user_subscription
                                          {:user_id user-id
                                           :group_name (or group-name ungroup)
                                           :title (or title (:title rss))
                                           :rss_link_id (:id rss)})]
             (info (str "user#" user-id) "add"
                   (str "(" (-> feeds :entries count) ")" link))
             (fdb/insert-rss-xml body)
             (fdb/save-feeds feeds (:id rss) user-id) ;; 3. save feeds
             (add-subscription-ret us rss (-> feeds :entries count)))
           {:status 460
            :message "Bad feedlink"}))

(defn add-subscription* [link user-id & options]
  (if-let [sub (db/fetch-rss-link {:url link})]
    ;; we have the subscription
    (apply add-exists-subscription sub user-id options)
    ;; first time subscription
    (apply create-subscripton link user-id options)))

(defn add-subscription [req]
  (let [link (:link *json-body*)
        user-id (:id *user*)]
    (add-subscription* link user-id)))

(defn get-subscription [req]
  (let [{:keys [id limit offset] :or {limit 20 offset 0}} (:params req)
        rss-id (:rss_link_id (db/fetch-subscription {:id (to-int id)}))]
    (when rss-id (fdb/fetch-feeds-for-user (:id *user*)
                                           rss-id
                                           (to-int limit)
                                           (to-int offset)))))

(defn get-overview* []
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

(defn get-overview [req]
  (get-overview*))

(defn customize-subscription [req]
  (let [user-id (:id *user*)]
    (db/update-subscription user-id (-> req :params :id to-int) *json-body*)))

(defn unsubscribe [req]
  (let [user-id (:id *user*)]
    (db/delete-subscription user-id (-> req :params :id to-int))))
