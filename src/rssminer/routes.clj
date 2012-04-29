(ns rssminer.routes
  (:use [compojure.core :only [defroutes GET POST DELETE ANY context]]
        (ring.middleware [keyword-params :only [wrap-keyword-params]]
                         [file-info :only [wrap-file-info make-http-format]]
                         [params :only [wrap-params]]
                         [session :only [wrap-session]]
                         [multipart-params :only [wrap-multipart-params]]
                         [file :only [wrap-file]]
                         [session :only [wrap-session]])
        (rssminer [middleware :only [wrap-auth wrap-cache-header
                                     wrap-failsafe
                                     wrap-request-logging-in-dev
                                     JPOST JPUT JDELETE JGET]]
                  [redis :only [redis-store]]))
  (:require [compojure.route :as route]
            [rssminer.import :as import]
            (rssminer.handlers [reader :as reader]
                               [subscriptions :as subs]
                               [proxy :as proxy]
                               [users :as user]
                               [dashboard :as dashboard]
                               [feeds :as feed])))

(defroutes api-routes
  (context "/subs" []
           (JGET "/" [] subs/list-subscriptions)
           (JPOST "/add" [] subs/add-subscription)
           (JGET "/:rss-id" [] feed/get-by-subscription)
           (JGET "/p/:rss-id" [] subs/polling-subscription)
           (JPOST "/:id" [] subs/customize-subscription)
           (JDELETE "/:id" [] subs/unsubscribe))
  (JGET "/search" [] reader/search)
  (JPOST "/settings" [] user/save-settings)
  (JGET "/welcome" [] user/summary)
  (context "/feeds/:id" []
           (JPOST "/vote" [] feed/user-vote)
           (JPOST "/read" [] feed/mark-as-read))
  (JPOST "/import/opml" [] import/opml-import)
  (JGET "/export/opml" [] "TODO"))

(defroutes all-routes
  (GET "/" [] reader/landing-page)
  (GET "/fav" [] proxy/get-favicon)
  (GET "/p" []  proxy/handle-proxy)
  (GET "/f/o/:id" [] proxy/proxy-feed)
  (GET "/a" [] reader/app-page)
  (JGET "/stat" [] dashboard/get-stat)
  (context "/dashboard" []
           (GET "/" [] reader/dashboard-page))
  (context "/login" []
           (GET "/" [] user/show-login-page)
           (POST "/" [] user/login)
           (GET "/google" [] user/google-openid)
           (GET "/checkauth" [] user/checkauth))
  (GET "/oauth2callback" [] import/oauth2callback)
  (GET "/import/google" [] import/greader-import)
  (context "/signup" []
           (GET "/" [] user/show-signup-page)
           (POST "/" [] user/signup))
  (context "/api" [] api-routes)
  (route/files "") ;; files under public folder
  (route/not-found "<h1>Page not found.</h1>" ))

;;; The last one in the list is the first one get the request,
;;; the last one get the response
(defn app []
  (-> #'all-routes
      wrap-auth
      (wrap-session {:store (redis-store (* 3600 24 3))
                     :cookie-name "rm_id"
                     :cookie-attrs {:http-only true}})
      wrap-cache-header
      wrap-request-logging-in-dev
      wrap-keyword-params
      wrap-multipart-params
      wrap-params
      wrap-failsafe))
