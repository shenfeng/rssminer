(ns rssminer.views.layouts
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

(deftemplate layout "templates/layout.html" [body]
  [:#main] (html/substitute body))
