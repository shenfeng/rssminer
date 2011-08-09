(ns rssminer.handlers.feedreader
  (:use [rssminer.handlers.subscriptions :only [get-overview*]])
  (:require [rssminer.views.feedreader :as view]))

(defn landing-page [req]
  (view/landing-page))

(defn index-page [req]
  (view/index-page {:overview (get-overview*)}))

(defn demo-page [req]
  (view/demo-page))

(defn expe-page [req]
  (view/expe-page))

(defn dashboard-page [req]
  (view/dashboard-page))
