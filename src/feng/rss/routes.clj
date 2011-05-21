(ns feng.rss.routes
  (:use [compojure.core :only [defroutes GET POST HEAD DELETE ANY context]]
        [ring.adapter.jetty :only (run-jetty)]
        [clojure.contrib.def :only [defnk]]
        (ring.middleware [keyword-params :only [wrap-keyword-params]]
                         [file-info :only [wrap-file-info]]
                         [params :only [wrap-params]]
                         [file :only [wrap-file]]
                         [cookies :only [wrap-cookies]])
        (feng.rss [middleware :only (wrap-reload-in-dev
                                     wrap-cache-header
                                     wrap-request-logging)]
                  [database :only [use-psql-database!]])
        (feng.rss.handlers [feedreader :as index]))
  (:require [feng.rss.config :as config]))

(let [views-ns '[feng.rss.views.feedreader]
      all-rss-ns (filter
                   #(re-find #"^feng" (str %)) (all-ns))
      ns-to-path (fn [clj-ns]
                   (str
                    (clojure.string/replace
                     (clojure.string/replace (str clj-ns) #"-" "_")
                     #"\." "/") ".clj"))
      src-path (fn [clj-ns]
                 {(str "src/" (ns-to-path clj-ns)) [(.getName clj-ns)]})
      src-ns-map (conj
                  (map src-path all-rss-ns)
                  {"src/templates" views-ns})]
  (def reload-meta
    (apply merge src-ns-map)))

(defroutes all-routes
  (GET "/" [] index/index-page)
  (ANY "*" [] {:status 404,
               :headers {"content-type" "text/html"}
               :body "<h1>Page not found.</h1>"}))

(defn app [] (-> #'all-routes
                 wrap-keyword-params
                 wrap-params
                 wrap-cookies
                 (wrap-reload-in-dev reload-meta)
                 (wrap-file "public")
                 wrap-cache-header                               
                 wrap-file-info
                 wrap-request-logging))

(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    (.stop @server)
    (reset! server nil)))

(defnk start-server [:jdbc-url "jdbc:postgresql://localhost/feedreader"
                     :db-user "postgres"
                     :db-password "123456"
                     :port 8080
                     :profile :development]
  (stop-server)
  (reset! config/env-profile profile)
  (use-psql-database! :jdbc-url jdbc-url
                      :user db-user
                      :password db-password)
  (reset! server (run-jetty (app) {:port port :join? false})))
