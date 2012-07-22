(ns rssminer.tools
  (:use me.shenfeng.mustache
        [rssminer.database :only [mysql-query with-mysql mysql-insert]]
        [clojure.tools.cli :only [cli]]
        [compojure.core :only [defroutes GET POST DELETE ANY context]]
        [clojure.tools.logging :only [info]]
        [ring.util.response :only [redirect]]
        [me.shenfeng.http.server :only [run-server]]
        (rssminer [database :only [use-mysql-database!
                                   close-global-mysql-factory!]]
                  [util :only [to-int]]
                  [config :only [rssminer-conf socks-proxy]])
        [clojure.java.io :only [resource]])
  (:require [compojure.route :as route]))

(deftemplate compare_tpl (slurp (resource "compare.tpl")))

(def step 5)
(defonce server (atom nil))

(defn- get-data [start]
  (mysql-query ["SELECT d.*, f.link from feed_data d
join feeds f on f.id = d.id
where d.id >= ? and d.id <= ?"  start (+ start step)]))

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

(defroutes all-routes
  (GET "/compare" [] compare-data)
  (route/files "") ;; files under public folder
  (route/not-found "<p>Page not found.</p>" ))

(defn stop-server []
  (when-not (nil? @server)
    (info "shutdown Rssminer server....")
    (@server)
    (reset! server nil))
  (close-global-mysql-factory!))

(defn start-server
  [{:keys [port index-path profile db-url worker
           db-user bind-ip]}]
  (stop-server)
  (use-mysql-database! db-url db-user)
  (swap! rssminer-conf assoc
         :profile profile
         :worker worker)
  (reset! server (run-server all-routes {:port port
                                         :ip bind-ip
                                         :thread worker})))

(defn -main [& args]
  "Start toolserver server"
  (let [[options _ banner]
        (cli args
             ["-p" "--port" "Port to listen" :default 9091 :parse-fn to-int]
             ["--worker" "Http worker thread count" :default 2
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
    (start-server options)))
