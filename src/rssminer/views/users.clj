(ns rssminer.views.users
  (:use [rssminer.views.layouts :only [layout snippet]])
  (:require [net.cgrand.enlive-html :as html]))

(let [s (snippet
         "templates/user/login.html" [html/root] [return-url]
         [:input#return-url] (html/set-attr :value return-url))]
  (defn login-page [return-url]
    (apply str (layout (s return-url)))))

(let [s (snippet "templates/user/signup.html" [html/root] [])]
  (defn signup-page []
    (apply str (layout (s)))))
