(ns rssminer.tmpls
  (:use me.shenfeng.mustache)
  (:require [clojure.java.io :as io]
            [rssminer.mesgs :as m]
            [rssminer.config :as cfg])
  (:import [me.shenfeng.mustache ResourceList Mustache]))

;;; templates/help.tpl => help
(defn- mapper [file]
  (let [content (slurp
                 (or (io/resource file)
                     (try (io/reader file)
                          (catch Exception e))))
        name (let [idx (.indexOf file "templates")
                   remain (.substring file (+ idx (count "templates") 1))]
               ;; drop extention
               (keyword (.substring remain 0
                                    (.lastIndexOf remain (int \.)))))]
    [name content]))

(def tmpls
  (apply hash-map
         (apply concat
                (map mapper (resources #".*templates/.*")))))



(defn- add-info [context]
  (let [zh? (or (= "zh" (-> context :req :params :lang))
                (re-find #"zh" (or (get-in context [:req :headers "accept-language"]) ""))
                false)
        context (assoc context
                  :dev (= (cfg/cfg :profile) :dev)
                  :zh? zh?)]
    (merge (if zh? m/zh-messages m/en-messages) context)))

(.clear Mustache/CACHE)       ; prevent OOM when dev
(deftemplates tmpls add-info)
