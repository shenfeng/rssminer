(ns rssminer.admin.admin
  (:use [me.shenfeng.http.server :only [defwshandler send-mesg on-close]]
        [clojure.tools.logging :only [info]]
        [rssminer.classify :only [on-feed-event]]
        [rssminer.util :only [defhandler to-int]]
        [rssminer.database :only [mysql-query]]
        [ring.util.response :only [redirect]]
        [compojure.core :only [GET routes]])
  (:require [rssminer.fetcher :as f]
            [rssminer.tmpls :as tmpl]))

(defn show-admin [req]
  (let [stat (sort-by :key (map (fn [[k v]] {:key (str k) :val v}) (f/fetcher-stat)))]
    (tmpl/admin {:stat stat
                 :fetcher (f/running?)})))

(defhandler fetcher [req command]
  (case command
    "stop" (f/stop-fetcher)
    "start" (f/start-fetcher))
  (redirect "/admin"))

(defhandler recompute-scores [req user-id]
  (if user-id
    (do
      (on-feed-event (to-int user-id) (to-int -1))
      user-id)
    (let [users (map :id (mysql-query ["select id from users"]))]
      (doseq [id users]
        (on-feed-event (to-int id) (to-int -1)))
      (map str (interpose ", " users)))))

(def admin-routes
  (let [handler (routes
                 (GET "/" [] show-admin)
                 (GET "/compute" [] recompute-scores)
                 ;; TODO POST should
                 (GET "/fetcher" [] fetcher))]
    (fn [req]
      (when (= 1 (:session req)) ;; 1 is myself, who is admin
        (handler req)))))
