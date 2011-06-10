(ns freader.views.users
  (:use [freader.views.layouts :only [layout]])
  (:require [net.cgrand.enlive-html :as html]))

(let [snippet (html/snippet
               "templates/user/login.html" [html/root] [return-url]
               [:input#return-url] (html/set-attr :value return-url))]
  (defn login-page [return-url]
    (apply str (layout (snippet return-url)))))

(let [snippet (html/snippet
               "templates/user/signup.html" [html/root] [])]
  (defn signup-page []
    (apply str (layout (snippet)))))
