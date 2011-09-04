(ns rssminer.setup-database
  (:use (rssminer [database :only [use-h2-database! close-global-h2-factory!
                                   import-h2-schema!]]
                  [config :only [ungroup]])
        (rssminer.db [user :only [create-user]]
                     [util :only [h2-query with-h2]])
        (clojure.tools [logging :only [info]]
                       [cli :only [cli optional required]])
        [clojure.java.jdbc :only [insert-record]])
  (:import java.io.File))

(defn setup [{:keys [index-path db-path password]}]
  (info "delete" db-path
        (.delete (File. (str db-path ".h2.db"))))
  (doall (map #(info "delete" % (.delete %))
              (reverse (file-seq (File. index-path)))))
  (use-h2-database! (str db-path ";PAGE_SIZE=8192"))
  (info "import h2 schema, create user feng")
  (import-h2-schema!)
  (let [user (create-user {:name "feng"
                           :password password
                           :email "shenedu@gmail.com"})
        rsses (h2-query ["select id from rss_links"])]
    (doseq [rss rsses]
      (with-h2
        (insert-record :user_subscription {:user_id (:id user)
                                           :rss_link_id (:id rss)
                                           :group_name ungroup}))))
  (close-global-h2-factory!))

(defn main [& args]
  "Setup rssminer database"
  (setup
   (cli args
        (required ["-p" "--password" "password"])
        (optional ["--db-path" "H2 Database path"
                   :default "/dev/shm/rssminer"])
        (optional ["--index-path" "Path to store lucene index"
                   :default "/dev/shm/rssminer-index"]))))
