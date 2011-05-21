(ns feng.rss.config)

(def env-profile (atom :production))

(defn in-prod? []
  (= @env-profile :production))

(defn in-dev? []
  (= @env-profile :development))
