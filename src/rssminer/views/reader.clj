(ns rssminer.views.reader
  (:use [rssminer.views.layouts :only [snippet deftemplate]]
        [rssminer.util :only [serialize-to-js]])
  (:require [net.cgrand.enlive-html :as html]))

(deftemplate app-page "templates/app.html" [data]
  [:head] (html/append (html/html-snippet (serialize-to-js data))))

(deftemplate landing-page "templates/landing.html" [] )
