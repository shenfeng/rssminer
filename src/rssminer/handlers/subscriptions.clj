(ns rssminer.handlers.subscriptions
  (:use (rssminer [redis :only [fetch-rss]]
                  [util :only [now-seconds defhandler valid-url?]])
        [rssminer.database :only [mysql-insert-and-return]]
        [clojure.tools.logging :only [info]])
  (:require [rssminer.db.subscription :as db]
            [rssminer.db.feed :as fdb]
            [clojure.string :as str]))

(defn subscribe [url uid title group-name]
  (when url
    (let [sub (or (db/fetch-rss-link-by-url url)
                  (mysql-insert-and-return :rss_links {:url url
                                                       :user_id uid}))]
      (fetch-rss sub)
      (if-let [us (db/fetch-subscription uid (:id sub))]
        us
        (mysql-insert-and-return :user_subscription
                                 {:user_id uid
                                  :group_name group-name
                                  :title title
                                  :rss_link_id (:id sub)})))))

(defhandler polling-fetcher [req rss-id uid]             ;; wait for fetcher return
  (db/fetch-sub uid rss-id))

(defhandler list-subscriptions [req uid]
  (db/fetch-subs uid))

(defhandler unread-count [req uid]
  (let [numbers (db/get-numbers uid (- (now-seconds) (* 3600 24 30)))
        unread (filter #(> (:unread %) 0) numbers)]
    (zipmap (map :id unread)
            (map :unread unread))))

(defhandler add-subscription [req uid]
  (let [{:keys [link g]}  (-> req :body)]
    (info (str "user: " uid " add sub: " link))
    ;; enqueue, client need to poll for result
    (if (valid-url? link)
      (subscribe link uid nil g)
      {:status 200 :body "not valid url"})))

(defhandler save-sort-order [req uid]
  (let [;; [{:g group :ids [id, id, id]}]
        data (mapcat (fn [{:keys [ids g]}]
                       (map (fn [id] {:g g :id id}) ids)) (:body req))]
    (db/update-sort-order uid data)
    {:status 204}))

(defhandler unsubscribe [req uid rss-id]
  (db/delete-subscription uid rss-id))
