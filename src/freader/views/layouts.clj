(ns freader.views.layouts
  (:require [net.cgrand.enlive-html :as html]
            [freader.config :as config]))

(html/deftemplate layout "templates/layout.html" [body]
  [:#main] (html/substitute body)
  [(html/attr= :data-profile "development")]
  (if (config/in-dev?) identity (html/substitute ""))
  [(html/attr= :data-profile "production")]
  (if (config/in-prod?) identity (html/substitute "")))
