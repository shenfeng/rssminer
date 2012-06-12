(ns rssminer.views.layouts
  (:use [rssminer.util :only [serialize-to-js]])
  (:require [net.cgrand.enlive-html :as html]
            [rssminer.config :as conf]
            [clojure.string :as str]))

(def ^{:private true} profile-specific
  `([(html/attr= :data-profile "dev")]
      (if (conf/in-dev?) identity (html/substitute ""))
      [(html/attr= :data-profile "prod")]
        (if (conf/in-prod?) identity (html/substitute ""))))

(defmacro snippet [source selector args & forms]
  (let [with-profile (concat profile-specific forms)]
    `(html/snippet ~source ~selector ~args ~@with-profile)))

(defmacro template [source args & forms]
  (let [with-profile (concat profile-specific forms)]
    `(html/template ~source ~args ~@with-profile)))

(defmacro deftemplate [name source args & forms]
  `(def ~name (template ~source ~args ~@forms)))

(defmacro defsnippet [name source selector args & forms]
  `(def ~name (snippet ~source ~selector ~args ~@forms)))

(deftemplate login-page "templates/user/login.html" [return-url]
  [:input#return-url] (html/set-attr :value return-url))

(deftemplate signup-page "templates/user/signup.html" [])

(deftemplate app-page "templates/app.html" [data]
  [:head] (html/append (html/html-snippet (serialize-to-js data))))

(deftemplate landing-page "templates/landing.html" [] )
