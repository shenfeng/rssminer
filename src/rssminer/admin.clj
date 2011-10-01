(ns rssminer.admin
  (:use (rssminer [database :only [use-h2-database! close-global-h2-factory!
                                   import-h2-schema!]]
                  [search :only [indexer index-feed]]
                  [config :only [ungroup]]
                  [util :only [ignore-error gen-snippet extract-text]])
        (rssminer.db [user :only [create-user]]
                     [feed :only [insert-tags]]
                     [util :only [h2-query with-h2 id-k]])
        (clojure.tools [logging :only [info]]
                       [cli :only [cli optional required]])
        [clojure.java.jdbc :only [insert-record with-query-results
                                  insert-record]])
  (:require [clojure.string :as str])
  (:import java.io.File
           java.sql.Clob
           rssminer.Searcher))

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

(defn rebuild-index []
  (.toggleInfoStream ^Searcher @indexer true)
  (.clear ^Searcher @indexer)
  (with-h2
    (with-query-results rs ["select * from feeds"]
      (doseq [feed rs]
        (let [feed (update-in feed [:summary]
                              #(slurp (.getCharacterStream ^Clob %)))]
          (index-feed (:id feed) (-> feed :summary extract-text) feed)))))
  (.toggleInfoStream ^Searcher @indexer false))

(defn export-data []
  (let [feeds (h2-query
               ["SELECT id, title, author, link, summary, snippet, tags
                          FROM feeds ORDER BY id LIMIT 2000"])
        links (h2-query ["SELECT domain, url, title FROM
                              crawler_links LIMIT 20000"])
        rss (h2-query ["SELECT title, url, description,
                             alternate FROM rss_links LIMIT 100000"])]
    (spit "/tmp/rssminer_data"
          (prn-str {:feeds feeds :links links :rss rss}))))

(defn import-data []
  (let [{:keys [feeds links rss]} (read-string (slurp "/tmp/rssminer_data"))]
    (doseq [l links]
      (ignore-error (with-h2 (insert-record :crawler_links l))))
    (doseq [r rss]
      (ignore-error (with-h2 (insert-record :rss_links r))))
    (doseq [feed feeds]
      (with-h2 (ignore-error
                (insert-record :feeds feed))))))

(defn main [& args]
  "Setup rssminer database"
  (setup
   (cli args
        (required ["-p" "--password" "password"])
        (optional ["--db-path" "H2 Database path"
                   :default "/dev/shm/rssminer"])
        (optional ["--index-path" "Path to store lucene index"
                   :default "/dev/shm/rssminer-index"]))))
