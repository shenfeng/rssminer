(ns rssminer.admin.admin
  (:use [me.shenfeng.http.server :only [defwshandler send-mesg on-close]]
        [clojure.tools.logging :only [info]]
        [rssminer.classify :only [on-feed-event]]
        [rssminer.util :only [defhandler to-int]]
        [rssminer.database :only [mysql-query]]
        [ring.util.response :only [redirect]]
        [compojure.core :only [GET routes]])
  (:require [rssminer.fetcher :as f]
            [clojure.string :as str]
            [rssminer.tmpls :as tmpl]))

(defn- key-str [k]
  (cond (string? k) k
        (keyword? k) (name k)
        :else (str k)))

(defn- display-map [m]
  (let [m (map (fn [[k v]] {:key (key-str k) :val v}) m)]
    (filter :val (sort-by :key m))))

(defn- table-stats []
  (let [tables (apply concat (map vals (mysql-query ["show tables"])))]
    (map (fn [table]
           (let [s (first (mysql-query [(str "show table status like '" table "'")]))]
             {:name (:name s)
              :stat (display-map
                     (select-keys s [:rows :avg_row_length :data_length
                                     :index_length :auto_increment]))}))
         tables)))

(defn show-admin [req]
  (tmpl/admin {:stat (display-map (f/fetcher-stat))
               :table-stats (table-stats)
               :fetcher (f/running?)}))

(defhandler fetcher [req command]
  (case command
    "stop" (f/stop-fetcher)
    "start" (f/start-fetcher))
  (redirect "/admin"))

(defhandler recompute-scores [req user-id]
  (if user-id
    (do
      (on-feed-event (to-int user-id) (int -1))
      user-id)
    (let [users (map :id (mysql-query ["select id from users"]))]
      (doseq [id users]
        (on-feed-event (to-int id) (int -1)))
      (str/join ", " users))))

(def admin-routes
  (let [handler (routes
                 (GET "/" [] show-admin)
                 (GET "/compute" [] recompute-scores)
                 ;; TODO POST should
                 (GET "/fetcher" [] fetcher))]
    (fn [req]
      (when (= 1 (:session req)) ;; 1 is myself, who is admin
        (handler req)))))
