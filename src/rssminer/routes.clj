(ns rssminer.routes
  (:use [compojure.core :only [defroutes GET POST DELETE ANY context]]
        (ring.middleware [keyword-params :only [wrap-keyword-params]]
                         [file-info :only [wrap-file-info]]
                         [params :only [wrap-params]]
                         [session :only [wrap-session]]
                         [multipart-params :only [wrap-multipart-params]]
                         [file :only [wrap-file]]
                         [session :only [wrap-session]])
        (rssminer [middleware :only [wrap-auth wrap-cache-header
                                     wrap-reload-in-dev wrap-failsafe
                                     wrap-request-logging-in-dev
                                     JPOST JPUT JDELETE JGET]]
                  [import :only [opml-import]]))
  (:require [clojure.string :as str]
            [compojure.route :as route]
            (rssminer.handlers [feedreader :as rssminer]
                               [subscriptions :as subscription]
                               [users :as user]
                               [dashboard :as dashboard]
                               [feeds :as feed]))
  (:import clojure.lang.Namespace))

(let [views-ns '[rssminer.views.feedreader
                 rssminer.views.layouts]
      all-rss-ns (filter
                  #(re-find #"^rssminer" (str %)) (all-ns))
      ns-to-path (fn [clj-ns]
                   (str
                    (str/replace
                     (str/replace (str clj-ns) #"-" "_")
                     #"\." "/") ".clj"))]
  (def reload-meta
    (apply merge (conj
                  (map (fn [clj-ns]
                         {(str "src/" (ns-to-path clj-ns))
                          [(.getName ^Namespace clj-ns)]}) all-rss-ns)
                  {"src/templates" views-ns}))))

(defroutes api-routes
  (context "/dashboard" []
           (JGET "/:section" [] dashboard/get-data)
           (JPOST "/" [] dashboard/settings))
  (context "/subscriptions" []
           (JGET "/overview" [] subscription/get-overview)
           (JPOST "/add" [] subscription/add-subscription)
           (JGET "/:id" [] subscription/get-subscription)
           (JPOST "/:id" [] subscription/customize-subscription)
           (JDELETE "/:id" [] subscription/unsubscribe))
  (JGET "/search" [] rssminer/search)
  (context "/feeds" []
           (context "/:feed-id" []
                    (JPOST "/pref" [] feed/save-pref)
                    (JPOST "/categories" [] "TODO")
                    (JDELETE "/categories" [] "TODO")
                    (JPOST "/comments" [] "TODO")
                    (JDELETE "/comments/:comment-id" [] "TODO")))
  (JPOST "/import/opml-import" [] opml-import)
  (JGET "/export/opml-export" [] "TODO"))

(defroutes all-routes
  (GET "/" [] rssminer/landing-page)
  (GET "/app" [] rssminer/index-page)
  (context "/browse" []
           (GET ["/:term" :term #".*"] [] rssminer/browse-feed))
  (context "/dashboard" []
           (GET "/" [] rssminer/dashboard-page))
  (context "/login" []
           (GET "/" [] user/show-login-page)
           (POST "/" [] user/login))
  (context "/signup" []
           (GET "/" [] user/show-signup-page)
           (POST "/" [] user/signup))
  (context "/api" [] api-routes)
  (route/files "") ;; files under public folder
  (route/not-found "<h1>Page not found.</h1>" ))

;;; The last one in the list is the first one get the request,
;;; the last one get the response
(defn app [] (-> #'all-routes
                 wrap-auth
                 wrap-session
                 wrap-cache-header
                 wrap-request-logging-in-dev
                 wrap-keyword-params
                 wrap-multipart-params
                 wrap-params
                 (wrap-reload-in-dev reload-meta)
                 wrap-failsafe))
