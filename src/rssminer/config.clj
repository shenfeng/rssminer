(ns rssminer.config)

(defonce env-profile (atom :dev))

(defn in-prod? []
  (= @env-profile :prod))

(defn in-dev? []
  (= @env-profile :dev))

(def ungroup "ungrouped")

(def crawler-threads-count 15)
