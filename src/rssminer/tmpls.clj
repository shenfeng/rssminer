(ns rssminer.tmpls
  (:use me.shenfeng.mustache)
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [rssminer.config :as cfg])
  (:import me.shenfeng.mustache.ResourceList))

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

(deftemplates tmpls (fn [context] (assoc context :dev (cfg/in-dev?))))

