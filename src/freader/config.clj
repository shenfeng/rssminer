(ns freader.config)

(def env-profile (atom :production))

(defn in-prod? []
  (= @env-profile :production))

(defn in-dev? []
  (= @env-profile :development))

(def ungroup "ungrouped")

(def DB_HOST
  (get (System/getenv) "READER_DB_HOST" "127.0.0.1"))
(def PSQL_USERNAME "postgres")
(def PSQL_PASSWORD "123456")
