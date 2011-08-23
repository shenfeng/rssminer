(ns rssminer.views.feedreader
  (:use [rssminer.views.layouts :only [layout snippet deftemplate]])
  (:use [rssminer.util :only [serialize-to-js]])
  (:require [net.cgrand.enlive-html :as html]))

(deftemplate index-page "templates/index.html" [data]
  [:head] (html/append (html/html-snippet (serialize-to-js data))))

(deftemplate landing-page "templates/landing.html" [] )

(deftemplate dashboard-page "templates/dashboard.html" [] )

(deftemplate browse-feed "templates/browse.html" [data]
  [:head] (html/append (html/html-snippet (serialize-to-js data))))
