(ns feng.rss.routes
  (:use [compojure.core :only [defroutes GET POST HEAD DELETE ANY context]]
        [ring.adapter.jetty :only (run-jetty)]
        [clojure.contrib.def :only [defnk]]
        (ring.middleware [keyword-params :only [wrap-keyword-params]]
                         [file-info :only [wrap-file-info]]
                         [params :only [wrap-params]]
                         [file :only [wrap-file]]
                         [session :only [wrap-session]])
        (feng.rss [middleware :only (wrap-reload-in-dev
                                     wrap-cache-header
                                     wrap-auth
                                     wrap-request-logging
                                     JPOST JPUT JDELETE JGET)]
                  [database :only [use-psql-database!]])
        [sandbar.stateful-session :only [wrap-stateful-session]])
  (:require [feng.rss.config :as config]
            (feng.rss.handlers [feedreader :as index]
                               [feeds :as feed]
                               [users :as user])))

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

(defroutes api-routes
  (JGET "/feeds/:fs-id" [] feed/get-feeds)
  (JPUT "/feedsource" [] feed/add-feedsource)) 

(defroutes all-routes
  (GET "/" [] index/index-page)
  ;; (GET "/login" [] "")
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
