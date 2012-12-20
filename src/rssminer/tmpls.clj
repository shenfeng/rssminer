(ns rssminer.tmpls
  (:use [me.shenfeng.mustache :only [gen-tmpls-from-resources]]
        [rssminer.util :only [ignore-error]])
  (:require [rssminer.i18n :as i]
            [rssminer.config :as cfg]))

(def csses {:app-css (ignore-error (slurp "public/css/app.css"))
            :landing-css (ignore-error (slurp "public/css/landing2.css"))})

(defn- add-info [context]
  (let [zh? (if-let [lang (-> i/*req* :params :lang)]
              (= "zh" lang)
              (if (re-find #"zh" (or (get-in i/*req* [:headers "accept-language"]) ""))
                true
                false))
        dev? (= (cfg/cfg :profile) :dev)
        context (assoc (merge (if zh? i/zh-messages i/en-messages) context)
                  :static-server (cfg/cfg :static-server)
                  :dev dev?
                  :zh? zh?)]
    (if dev? context (merge context csses))))

;;; templates/help.tpl => help
(gen-tmpls-from-resources "templates" [".tpl"] add-info)