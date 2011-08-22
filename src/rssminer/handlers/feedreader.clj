(ns rssminer.handlers.feedreader
  (:use [rssminer.handlers.subscriptions :only [get-overview*]]
        [rssminer.util :only [session-get]])
  (:require [rssminer.views.feedreader :as view]))

(defn landing-page [req]
  (view/landing-page))

(defn index-page [req]
  (view/index-page {:overview (get-overview* (:id (session-get req :user)))}))

(defn dashboard-page [req]
  (view/dashboard-page))
