(ns rssminer.tmpls
  (:use [me.shenfeng.mustache :only [gen-tmpls-from-resources]]
        [rssminer.util :only [ignore-error]])
  (:require [rssminer.i18n :as i]
            [rssminer.config :as cfg]))

(def resources {:app-css (ignore-error (slurp "public/css/app.css"))
                :admin-css (ignore-error (slurp "public/css/admin.css"))
                :mobile-l-css (ignore-error (slurp "public/css/l.css"))
                :landing-css (ignore-error (slurp "public/css/landing.css"))
                :landing-js (ignore-error (slurp "public/js/landing-min.js"))})

(defn- lang-zh? []
  (if-let [lang (-> i/*req* :params :lang)]
    (= "zh" lang)                       ; use choose one
    (when-let [ua (get-in i/*req*  [:headers "user-agent"])]
      (if (re-find #"baidu" ua)
        true                            ; baidu is ch
        (if (re-find #"zh" (or (get-in i/*req* [:headers "accept-language"]) ""))
          true                          ; browser language
          false)))))

(defn- add-info [context]
  (let [zh? (lang-zh?)
        dev? (= (cfg/cfg :profile) :dev)
        context (assoc (merge (if zh? i/zh-messages i/en-messages) context)
                  :static-server (cfg/cfg :static-server)
                  :server-host (get-in i/*req* [:headers "host"])
                  :dev dev?
                  :zh? zh?)]
    (if dev? context (merge context resources))))

;;; templates/help.tpl => help
(gen-tmpls-from-resources "templates" [".tpl"] add-info)