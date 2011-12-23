(ns rssminer.views.reader
  (:use [rssminer.views.layouts :only [layout snippet deftemplate]])
  (:use [rssminer.util :only [serialize-to-js]])
  (:require [net.cgrand.enlive-html :as html]))

(deftemplate v1-page "templates/app-v1.html" [data]
  [:head] (html/append (html/html-snippet (serialize-to-js data))))

(deftemplate app-page "templates/app.html" [])

(deftemplate landing-page "templates/landing.html" [] )

(deftemplate dashboard-page "templates/dashboard.html" [] )

