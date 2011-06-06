(ns feng.rss.views.feedreader
  (:use [feng.rss.views.layouts :only [layout]])
  (:require [net.cgrand.enlive-html :as html]))

(let [snippet (html/snippet
               "templates/index.html" [:div#main] [])]
  (defn index-page []
    (apply str (layout (snippet)))))

(let [snippet (html/snippet
               "templates/demo.html" [:div#main] [])]
  (defn demo-page []
    (apply str (layout (snippet)))))

(let [snippet (html/snippet
               "templates/expe.html" [:div#main] [])]
  (defn expe-page []
    (apply str (layout (snippet)))))

