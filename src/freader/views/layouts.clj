(ns freader.views.layouts
  (:require [net.cgrand.enlive-html :as html]))

(html/deftemplate layout "templates/layout.html" [body]
  [:#main] (html/substitute body))
