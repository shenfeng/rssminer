(ns rssminer.admin.admin
  (:use [me.shenfeng.http.server :only [defwshandler send-mesg on-close]]
        [clojure.tools.logging :only [info]]
        [rssminer.config :only [rssminer-conf]]
        [rssminer.classify :only [on-feed-event]]
        [rssminer.util :only [defhandler to-int]]
        [rssminer.database :only [mysql-query]]
        [ring.util.response :only [redirect]]
        [compojure.core :only [GET routes POST]])
  (:require [rssminer.fetcher :as f]
            [rssminer.search :as s]
            [rssminer.classify :as c]
            [rssminer.db.subscription :as sdb]
            [clojure.string :as str]
            [rssminer.tmpls :as tmpl]))

(defn- key-str [k]
  (cond (string? k) k
        (keyword? k) (name k)
        :else (str k)))

(defn- display-map [m]
  (let [m (map (fn [[k v]] {:key (key-str k) :val v}) m)]
    (filter :val (sort-by :key m))))

(defn show-db-stat [req]
  (let [tables (apply concat (map vals (mysql-query ["show tables"])))
        data (map (fn [table]
                    (let [s (first (mysql-query
                                    [(str "show table status like '" table "'")]))]
                      {:name (:name s)
                       :stat (display-map
                              (select-keys s [:rows :avg_row_length :data_length
                                              :index_length :auto_increment]))}))
                  tables)]
    (tmpl/admin-db {:table-stats data})))

(defn show-admin [req]
  (tmpl/admin-index {:stat (display-map (f/fetcher-stat))
                     :subs (mysql-query ["select id, total_feeds, url, error_msg, user_id
 from rss_links order by id desc limit 12"])}))

(defn show-feedback [req]
  (tmpl/admin-feedback
   {:feedbacks (mysql-query
                ["select * from feedback order by added_ts desc limit 10"])}))

(defhandler show-control [req]
  (tmpl/admin-control {:fetcher (f/running?)
                       :classfier (c/running?)
                       :searcher (s/running?)}))

(defhandler handle-control [req kind command]
  (if-not (str/blank? command)
    (case kind
      "searcher" (case command
                   "stop" (do (f/stop-fetcher)
                              (s/close-global-index-writer!))
                   "start" (s/use-index-writer!))
      "classfier" (case command
                    "stop" (do (f/stop-fetcher)
                               (c/stop-classify-daemon!))
                    "start" (c/start-classify-daemon!))
      "fetcher" (case command
                  "stop" (f/stop-fetcher)
                  "start" (f/start-fetcher))
      "score" (case command
                "all" (let [users (map :id (mysql-query ["select id from users"]))]
                        (doseq [id users]
                          (on-feed-event (to-int id) (int -1)))
                        (str/join ", " users))
                (on-feed-event (to-int command) (int -1)))
      "subs" (when-let [sub (sdb/fetch-rss-link-by-id command)]
               (rssminer.redis/fetch-rss sub))))
  (redirect "/admin/control"))

(defn show-config [req]
  (tmpl/admin-config {:configs (display-map @rssminer-conf)}))

(def admin-routes
  (let [handler (routes
                 (GET "/" [] show-admin)
                 (GET "/feedback" [] show-feedback)
                 (GET "/config" [] show-config)
                 (GET "/db" [] show-db-stat)
                 (GET "/control" [] show-control)
                 (POST "/control" [] handle-control))]
    (fn [req]
      (when (= 1 (:session req)) ;; 1 is myself, who is admin
        (handler req)))))
