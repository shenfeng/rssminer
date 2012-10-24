(ns rssminer.tools
  (:use me.shenfeng.mustache
        [clojure.tools.cli :only [cli]]
        [compojure.core :only [defroutes GET POST DELETE ANY context]]
        (ring.middleware [keyword-params :only [wrap-keyword-params]]
                         [params :only [wrap-params]])
        [clojure.tools.logging :only [info]]
        [ring.util.response :only [redirect]]
        [me.shenfeng.http.server :only [run-server]]
        (rssminer [database :only [use-mysql-database!
                                   close-global-mysql-factory!]]
                  [util :only [to-int]]
                  [database :only [mysql-query with-mysql mysql-insert]]
                  [search :only [use-index-writer! close-global-index-writer!]]
                  [config :only [rssminer-conf socks-proxy]])
        [clojure.java.io :only [resource]])
  (:require [compojure.route :as route])
  (:import rssminer.NearDuplicate))

(deftemplate compare_tpl (slurp (resource "compare.tpl")))
(deftemplate similar_tpl (slurp (resource "near_duplicate.tpl")))

(deftemplate dedup_sub (slurp (resource "dedup_sub.tpl")))

(def step 5)
(defonce server (atom nil))

(defn- get-data [start]
  (mysql-query ["SELECT d.*, f.link, f.title from feed_data d
join feeds f on f.id = d.id
where d.id >= ?  limit ?" start step]))

(defn- fetch-data-by-id [id]
  (first (mysql-query ["SELECT d.*, f.link, f.title, f.rss_link_id
from feed_data d join feeds f on f.id = d.id where d.id =? " id])))

(defn compare-data [req]
  (let [start (Integer/parseInt (or (-> req :params :start) "0"))
        data (map (fn [d]
                    (assoc d
                      :summary_length (-> d :summary count)
                      :compact_lenght (-> d :compact count)))
                  (get-data start))]

    (if (seq data)
      (to-html compare_tpl {:feeds data
                            :links
                            (range (max (- start 60) 0)
                                   (+ start 60) step)})
      (redirect (str "/compare?start=" (+ start (* 100 step)))))))

(defn find-it? [duplicates]
  (> (count duplicates) 1))

(def distance 2)

;;; TODO, not perfect 2599, 193870[2]CZ.
(defn find-silimar [req]
  (let [id (Integer/parseInt (or (-> req :params :id) "0"))
        duplicates (filter #(> % 0) (NearDuplicate/similar id distance))]
    (if (find-it? duplicates)
      (to-html similar_tpl {:article (fetch-data-by-id id)
                            :pages (range id (+ id 10))
                            :similars (map fetch-data-by-id duplicates)})
      (loop [i (inc id)]
        (if (find-it? (filter #(> % 0) (NearDuplicate/similar i distance)))
          (redirect (str "/s?id=" i))
          (recur (inc i)))))))

(defn simhash-in-sub [rssid]
  (filter (fn [[k v]] (> (count v) 1))
          (reduce (fn [m item]
                    (let [hash (:simhash item)]
                      (if (= hash -1)
                        m (assoc m hash
                                 (conj (get m hash []) (:id item))))))
                  {}
                  (mysql-query ["select id, simhash from feeds
                               where rss_link_id = ?" rssid]))))

(defn find-silimar-in-sub [req]
  (let [id (Integer/parseInt (or (-> req :params :id) "0"))
        dup (simhash-in-sub id)]
    (if (seq dup)
      (to-html dedup_sub {:sections (map (fn [[simash ids]]
                                           {:subs (map fetch-data-by-id ids)}) dup)
                          :pages (range id (+ id 10))})
      (loop [i id]
        (if (seq (simhash-in-sub i))
          (redirect (str "/sub?id=" i))
          (recur (inc i)))))))

(defroutes all-routes
  (GET "/s" [] find-silimar)
  (GET "/sub" [] find-silimar-in-sub)
  (GET "/compare" [] compare-data)
  (route/files "/static" {:root "test/public"}) ;; files under public folder
  (route/not-found "<p>Page not found.</p>" ))

(defn app []
  (-> #'all-routes
      wrap-keyword-params
      wrap-params))

(defn stop-server []
  (when-not (nil? @server)
    (info "shutdown Rssminer server....")
    (@server)
    (reset! server nil))
  (close-global-mysql-factory!)
  (close-global-index-writer!))

(defn start-server
  [{:keys [port index-path profile db-url worker
           db-user bind-ip]}]
  (stop-server)
  (use-mysql-database! db-url db-user)
  (use-index-writer! index-path)
  (swap! rssminer-conf assoc
         :profile profile
         :worker worker)
  (reset! server (run-server (app) {:port port
                                    :ip bind-ip
                                    :thread worker})))

(defn -main [& args]
  "Start toolserver server"
  (let [[options _ banner]
        (cli args
             ["-p" "--port" "Port to listen" :default 9091 :parse-fn to-int]
             ["--worker" "Http worker thread count" :default 4
              :parse-fn to-int]
             ["--redis-host" "Redis for session store"
              :default "127.0.0.1"]
             ["--db-url" "MySQL Database url"
              :default "jdbc:mysql://localhost/rssminer"]
             ["--db-user" "MySQL Database user name" :default "feng"]
             ["--bind-ip" "Which ip to bind" :default "0.0.0.0"]
             ["--index-path" "Path to store lucene index"
              :default "/var/rssminer/index"]
             ["--[no-]help" "Print this help"])]
    (when (:help options) (println banner) (System/exit 0))
    (reset! server (start-server options))
    ;; (NearDuplicate/init)
    ))
