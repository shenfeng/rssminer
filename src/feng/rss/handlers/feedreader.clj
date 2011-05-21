(ns feng.rss.handlers.feedreader
  (:require [feng.rss.views.feedreader :as view]))

(defn index-page [req]
  (apply str (view/index-page)))
