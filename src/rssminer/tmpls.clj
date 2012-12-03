(ns rssminer.tmpls
  (:use me.shenfeng.mustache)
  (:require [clojure.java.io :as io]
            [rssminer.i18n :as i]
            [rssminer.config :as cfg]
            [clojure.string :as str])
  (:import [me.shenfeng.mustache ResourceList Mustache]))

;;; templates/help.tpl => help

(defn- get-content [file]
  (str/replace (slurp (or (io/resource file)
                          (try (io/reader file) (catch Exception e))))
               ;; remove extra space
               #">\W<" "><"))

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

(defn- add-info [context]
  (let [zh? (if-let [lang (-> i/*req* :params :lang)]
              (= "zh" lang)
              (if (re-find #"zh" (or (get-in i/*req* [:headers "accept-language"]) ""))
                true
                false))
        context (assoc context
                  :static-server (cfg/cfg :static-server)
                  :dev (= (cfg/cfg :profile) :dev)
                  :zh? zh?)]
    (merge (if zh? i/zh-messages i/en-messages) context)))

(.clear Mustache/CACHE)       ; prevent OOM when dev

(deftemplates tmpls add-info)
