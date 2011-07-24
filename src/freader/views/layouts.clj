(ns freader.views.layouts
  (:require net.cgrand.enlive-html
            freader.config))

(defmacro snippet [source selector args & forms]
  (let [profile '([(net.cgrand.enlive-html/attr= :data-profile "dev")]
                    (if (freader.config/in-dev?) identity
                        (net.cgrand.enlive-html/substitute ""))
                    [(net.cgrand.enlive-html/attr= :data-profile "prod")]
                    (if (freader.config/in-prod?) identity
                        (net.cgrand.enlive-html/substitute "")))
        with-profile (concat profile forms)]
    `(net.cgrand.enlive-html/snippet ~source ~selector ~args ~@with-profile)))

(defmacro template [source args & forms]
  (let [profile '([(net.cgrand.enlive-html/attr= :data-profile "dev")]
                    (if (freader.config/in-dev?) identity
                        (net.cgrand.enlive-html/substitute ""))
                    [(net.cgrand.enlive-html/attr= :data-profile "prod")]
                    (if (freader.config/in-prod?) identity
                        (net.cgrand.enlive-html/substitute "")))
        with-profile (concat profile forms)]
    `(net.cgrand.enlive-html/template ~source ~args ~@with-profile)))

(defmacro deftemplate [name source args & forms]
  `(def ~name (template ~source ~args ~@forms)))

(defmacro defsnippet [name source selector args & forms]
  `(def ~name (snippet ~source ~selector ~args ~@forms)))

(deftemplate layout "templates/layout.html" [body]
  [:#main] (net.cgrand.enlive-html/substitute body))
