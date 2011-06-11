(ns freader.views.feedreader
  (:use [freader.views.layouts :only [layout]])
  (:require [net.cgrand.enlive-html :as html]
            [freader.config :as config]))

(let [snippet (html/snippet
               "templates/index.html" [:div#main] []
               [(html/attr= :data-profile "development")]
               (if (config/in-dev?) identity (html/substitute ""))
               [(html/attr= :data-profile "production")]
               (if (config/in-prod?) identity (html/substitute "")))]
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

