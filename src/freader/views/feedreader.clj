(ns freader.views.feedreader
  (:use [freader.views.layouts :only [layout snippet deftemplate]])
  (:require [freader.config :as config]))

(let [s (snippet "templates/index.html" [:div#main] [])]
  (defn index-page []
    (apply str (layout (s)))))

(deftemplate landing-page "templates/landing.html" [] )

(let [s (snippet "templates/demo.html" [:div#main] [])]
  (defn demo-page []
    (apply str (layout (s)))))

(let [s (snippet "templates/expe.html" [:div#main] [])]
  (defn expe-page []
    (apply str (layout (s)))))
