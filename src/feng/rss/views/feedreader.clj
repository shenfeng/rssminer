(ns feng.rss.views.feedreader
  (:use [feng.rss.views.layouts :only [layout]])
  (:require [net.cgrand.enlive-html :as html]))

(let [snippet (html/snippet
               "templates/index.html" [:div#main] [])]
  (defn index-page []
    (apply str (layout (snippet)))))
