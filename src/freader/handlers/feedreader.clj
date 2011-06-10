(ns freader.handlers.feedreader
  (:require [freader.views.feedreader :as view]))

(defn index-page [req]
  (view/index-page))

(defn demo-page [req]
  (view/demo-page))

(defn expe-page [req]
  (view/expe-page))
