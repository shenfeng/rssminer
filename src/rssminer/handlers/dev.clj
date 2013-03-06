(ns  rssminer.handlers.dev
  (:use [clojure.tools.logging :only [info]]
        rssminer.config
        [rssminer.middleware :as JGET]
        [compojure.core :only [defroutes GET routes]])
  (:require [clojure.string :as str]))

(defn reload-file [req]
  (let [f ^String (-> req :params :f)
        ns (if (.endsWith f "tpl") 'rssminer.tmpls
               (symbol (str/replace (str/replace
                                     (str/replace f #".+src/" "")
                                     #"\.\w+$" "")
                                    #"/" ".")))
        start (System/currentTimeMillis)]
    (require :reload ns)
    (str "reload " ns " takes " (- (System/currentTimeMillis) start) "ms")))

(def dev-routes
  (let [handler (routes (JGET "/changed" [] reload-file))]
    (fn [req]
      (when (= (cfg :profile) :dev)
        (handler req)))))
