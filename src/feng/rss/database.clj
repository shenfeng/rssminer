
(ns feng.rss.database
  (:import (clojure.lang RT)
           (org.apache.commons.dbcp BasicDataSource)))

(def *factory* {:factory nil
              :ds nil})

(defn close-global-psql-factory []
  (if-let [ds (:ds *factory*)]
    (.close ds)))

(defn use-psql-database! [& {:keys [jdbc-url user password] :as opts}]
  (do
    (RT/loadClassForName "org.postgresql.Driver")
    ;; TODO investigate other connection pool options, eg: BoneCP
    (let [ds (doto (BasicDataSource.)
               (.setUrl jdbc-url)
               (.setUsername user)
               (.setPassword password))
          f (fn [& args]  (.getConnection ds))]
      (close-global-psql-factory)
      (def *factory* {:factory f
                      :ds ds}))))
