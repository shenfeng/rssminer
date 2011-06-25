(ns freader.routes
  (:use [compojure.core :only [defroutes GET POST HEAD DELETE ANY context]]
        [ring.adapter.jetty7 :only [run-jetty]]
        [clojure.contrib.def :only [defnk]]
        (ring.middleware [keyword-params :only [wrap-keyword-params]]
                         [file-info :only [wrap-file-info]]
                         [params :only [wrap-params]]
                         [file :only [wrap-file]]
                         [session :only [wrap-session]])
        (freader [middleware :only (wrap-auth
                                    wrap-content-type
                                    wrap-cache-header
                                    wrap-failsafe
                                    wrap-request-logging
                                    wrap-reload-in-dev
                                    JPOST JPUT JDELETE JGET)]
                 [database :only [use-psql-database!]])
        [sandbar.stateful-session :only [wrap-stateful-session]])
  (:require [freader.config :as config]
            [clojure.string :as str]
            (freader.handlers [feedreader :as freader]
                              [feeds :as feed]
                              [users :as user])))

(let [views-ns '[freader.views.feedreader
                 freader.views.layouts]
      all-rss-ns (filter
                  #(re-find #"^freader" (str %)) (all-ns))
      ns-to-path (fn [clj-ns]
                   (str
                    (str/replace
                     (str/replace (str clj-ns) #"-" "_")
                     #"\." "/") ".clj"))
      src-path (fn [clj-ns]
                 {(str "src/" (ns-to-path clj-ns)) [(.getName clj-ns)]})
      src-ns-map (conj
                  (map src-path all-rss-ns)
                  {"src/templates" views-ns})]
  (def reload-meta
    (apply merge src-ns-map)))

(defroutes api-routes
  (JPOST "/subscription" [] feed/add-subscription)
  (JGET "/subscription/:id" [] feed/get-subscription)
  (JPOST "/subscription/:id" [] feed/customize-subscription)
  (JGET "/overview" [] feed/get-overview))

(defroutes all-routes
  (GET "/" [] freader/index-page)
  (GET "/demo" [] freader/demo-page)
  (GET "/expe" [] freader/expe-page)
  (context "/login" []
           (GET "/" [] user/show-login-page)
           (POST "/" [] user/login))
  (context "/signup" []
           (GET "/" [] user/show-signup-page)
           (POST "/" [] user/signup))
  (context "/api" [] api-routes)
  (ANY "*" [] {:status 404,
               :headers {"content-type" "text/html"}
               :body "<h1>Page not found.</h1>"}))

(defn app [] (-> #'all-routes
                 wrap-keyword-params
                 wrap-params
                 wrap-auth
                 wrap-stateful-session
                 (wrap-file "public")
                 wrap-cache-header
                 wrap-file-info
                 wrap-content-type
                 wrap-request-logging
                 (wrap-reload-in-dev reload-meta)
                 wrap-failsafe))

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
  (use-psql-database! jdbc-url
                      db-user
                      db-password)
  (reset! server (run-jetty (app) {:port port :join? false})))
