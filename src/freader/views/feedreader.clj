(ns freader.views.feedreader
  (:use [freader.views.layouts :only [layout snippet deftemplate]])
  (:use [freader.util :only [serialize-to-js]])
  (:require [net.cgrand.enlive-html :as html]))

(deftemplate index-page "templates/index.html" [data]
  [:head] (html/append (html/html-snippet (serialize-to-js data))))

(deftemplate landing-page "templates/landing.html" [] )

(let [s (snippet "templates/demo.html" [:div#main] [])]
  (defn demo-page []
    (apply str (layout (s)))))

(let [s (snippet "templates/expe.html" [:div#main] [])]
  (defn expe-page []
    (apply str (layout (s)))))
