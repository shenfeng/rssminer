(ns rssminer.handlers.reader
  (:use [rssminer.util :only [session-get to-int]]
        [rssminer.search :only [search*]]
        [rssminer.db.feed :only [fetch-unread-meta fetch-unread]]
        [rssminer.db.subscription :only [fetch-subs-by-user]])
  (:require [rssminer.views.reader :as view]
            [rssminer.config :as cfg]))

(defn landing-page [req]
  (view/landing-page))

(defn- compute-by-time [unread]
  (let [[day week month] (rssminer.time/time-pairs)
        k-day (str "d_" day)
        k-week (str "w_" week)
        k-month (str "m_" month)]
    (persistent! (reduce (fn [m {:keys [published_ts] :as i}]
                           (let [published_ts (or published_ts (+ 1 month))
                                 key (cond (> published_ts day) k-day
                                           (> published_ts week) k-week
                                           (> published_ts month) k-month
                                           :else "older")]
                             (assoc! m key (inc (get m key 0)))))
                         (transient {}) unread))))

(defn compute-by-sub [unread]
  (persistent! (reduce (fn [m {:keys [rss_link_id] :as i}]
                         (assoc! m rss_link_id
                                 (inc (get m rss_link_id 0))))
                       (transient {}) unread)))

(defn app-page [req]
  (view/app-page))

(defn v1-page [req]
  (let [user (session-get req :user)
        user-id (:id user)
        unread (fetch-unread-meta user-id)]
    (view/v1-page {:user user
                   :by_sub (compute-by-sub unread)
                   :by_time (compute-by-time unread)
                   :subs (fetch-subs-by-user user-id)
                   :feeds (fetch-unread user-id 7 1)})))

(defn dashboard-page [req]
  (view/dashboard-page))

(defn search [req]
  (let [{:keys [term limit] :or {limit 20}} (:params req)]
    (search* term (to-int limit) :user-id (:id (session-get req :user)))))
