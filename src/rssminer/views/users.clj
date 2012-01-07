(ns rssminer.views.users
  (:use [rssminer.views.layouts :only [snippet deftemplate]])
  (:require [net.cgrand.enlive-html :as html]))

(deftemplate login-page "templates/user/login.html" [return-url]
  [:input#return-url] (html/set-attr :value return-url))

(deftemplate signup-page "templates/user/signup.html" [])

