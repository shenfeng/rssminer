(ns rssminer.routes
  (:use [compojure.core :only [defroutes GET POST DELETE ANY context]]
        ring.middleware.session.store
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
                                     JPOST JPUT JDELETE JGET]]))
  (:require [compojure.route :as route]
            [rssminer.import :as import]
            rssminer.admin
            (rssminer.handlers [reader :as reader]
                               [subscriptions :as subs]
                               [proxy :as proxy]
                               [users :as user]
                               [feeds :as feed])))

(defroutes api-routes
  (context "/subs" []
           (JGET "/" [] subs/list-subscriptions)
           (JGET "/:rid"  [] feed/get-by-subscription)
           (JPOST "/sort" [] subs/save-sort-order)
           (JPOST "/add" [] subs/add-subscription)
           (JGET "/p/:rss-id" [] subs/polling-fetcher)
           (JDELETE "/:rss-id" [] subs/unsubscribe))
  (JGET "/search" [] reader/search)
  (JPOST "/settings" [] user/save-settings)
  (JGET "/welcome" [] user/summary)
  (context "/feeds/:id" []
           (JGET "/" [] feed/get-feed)
           (JPOST "/vote" [] feed/user-vote)
           (POST "/read" [] feed/mark-as-read)))

(defroutes all-routes
  (GET "/" [] reader/show-landing-page)
  (GET "/fav" [] proxy/get-favicon)
  (GET "/p" []  proxy/handle-proxy)
  (GET "/browser" []  reader/show-unsupported-page)
  (GET "/f/o/:id" [] proxy/proxy-feed)
  (GET "/a" [] reader/show-app-page)
  (GET "/demo" [] reader/show-demo-page)
  (context "/login" []
           (GET "/" [] user/show-login-page)
           (POST "/" [] user/login)
           (GET "/google" [] user/google-openid)
           (GET "/checkauth" [] user/checkauth))
  (GET "/admin/compute" [] rssminer.admin/recompute-scores)
  (GET "/oauth2callback" [] import/oauth2callback)
  (GET "/import/google" [] import/greader-import)
  (context "/signup" []
           (GET "/" [] user/show-signup-page)
           (POST "/" [] user/signup))
  (GET "/logout" [] user/logout)
  (context "/api" [] api-routes)
  (route/files "") ;; files under public folder
  (route/not-found "<p>Page not found.</p>" ))

(defn gen-key [data]
  (if-let [id (:id data)]
    (rssminer.Utils/encrytUserID (int id))
    "__noop__"))

(defn decode-key [^String key]
  (when key (try (rssminer.Utils/descryUserID key)
                 (catch Exception e))))

(deftype CookieSessionStore []
  SessionStore
  (read-session [_ key]
    (decode-key key))
  (write-session [_ key data]
    (or key (gen-key data)))
  (delete-session [_ key] "")) ;; noop

;;; The last one in the list is the first one get the request,
;;; the last one get the response
(defn app []
  (-> #'all-routes
      wrap-auth
      (wrap-session {:store (CookieSessionStore.)
                     :cookie-name "_id"
                     :cookie-attrs {:http-only true}})
      wrap-cache-header
      wrap-request-logging-in-dev
      wrap-keyword-params
      ;; wrap-multipart-params
      wrap-params
      wrap-failsafe))
