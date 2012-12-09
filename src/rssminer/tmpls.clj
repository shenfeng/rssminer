(ns rssminer.tmpls
  (:use me.shenfeng.mustache
        [rssminer.util :only [ignore-error]])
  (:require [clojure.java.io :as io]
            [rssminer.i18n :as i]
            [rssminer.config :as cfg]
            [clojure.string :as str])
  (:import [me.shenfeng.mustache ResourceList Mustache]))

;;; templates/help.tpl => help

(defn- get-content [file]
  (str/replace
   (str/replace
    (str/replace
     (slurp (or (io/resource file)
                (try (io/reader file) (catch Exception e))))
     ;; remove extra space
     #">\s*<" "><")
    #"}\s*<" "}<")
   #">\s*\{" ">{"))

(defn- mapper [^String file]
  (let [name (let [idx (.indexOf file "templates")
                   remain (.substring file (+ idx (count "templates") 1))]
               ;; drop extention
               (keyword (.substring remain 0
                                    (.lastIndexOf remain (int \.)))))]
    [name (get-content file)]))

(def tmpls
  (apply hash-map
         (apply concat
                (map mapper (resources #".*templates/.*")))))

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

(.clear Mustache/CACHE)       ; prevent OOM when dev

(deftemplates tmpls add-info)
