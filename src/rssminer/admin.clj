(ns rssminer.admin
  (:use (rssminer [database :only [use-h2-database! close-global-h2-factory!
                                   import-h2-schema!]]
                  [search :only [indexer index-feed use-index-writer!]]
                  [http :only [clean-resolve]]
                  [config :only [ungroup]]
                  [util :only [ignore-error gen-snippet extract-text to-int]])
        (rssminer.db [user :only [create-user]]
                     [feed :only [fetch-rss-links]]
                     [util :only [h2-query with-h2 h2-insert]])
        (clojure.tools [logging :only [info]]
                       [cli :only [cli]])
        [clojure.java.jdbc :only [insert-record with-query-results
                                  insert-record delete-rows]])
  (:require [clojure.string :as str])
  (:import java.io.File
           java.net.URI
           java.sql.Clob
           rssminer.Searcher))

(defn setup-db [{:keys [index-path db-path password]}]
  (info "delete" db-path
        (.delete (File. (str db-path ".h2.db"))))
  (doall (map #(info "delete" % (.delete %))
              (reverse (file-seq (File. index-path)))))
  (use-h2-database! db-path)
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


(defn rand-subscribe []
  (let [rrs (filter identity
                    (map #(first (h2-query ["select id, title from rss_links
                        where title is not NULL and id > ? limit 1" %]))
                         (set (repeatedly 45 #(rand-int 10000)))))]
    (doseq [r rrs]
      (ignore-error (h2-insert :user_subscription
                               {:user_id 1
                                :rss_link_id (:id r)
                                :title (:title r)})))))

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

(defn export-data [{:keys [data-path] :or {data-path "/tmp/rssminer_data"}}]
  (let [feeds (h2-query
               ["SELECT id, title, author, link, summary, snippet, tags
                          FROM feeds ORDER BY id LIMIT 2000"])
        links (h2-query ["SELECT domain, url, title FROM
                              crawler_links LIMIT 20000"])
        rss (h2-query ["SELECT title, url, description,
                             alternate FROM rss_links LIMIT 100000"])]
    (spit data-path
          (prn-str {:feeds feeds :links links :rss rss}))))

(defn clean-rss-links []
  (let [rsses (h2-query ["select id, url from rss_links"])
        c (atom 0)]
    (doseq [{:keys [url id]} rsses]
      (let [u (clean-resolve url "")]
        (when (nil? u)
          (swap! c inc)
          (println "delete " id url)
          (with-h2
            (delete-rows :rss_links ["id = ?" id])))))
    (println "count" @c)))

(defn clean-crawler-links []
  (let [rsses (h2-query ["select id, url from crawler_links"])
        c (atom 0)]
    (doseq [{:keys [id url]} rsses]
      (let [u (clean-resolve url "")]
        (when (nil? u)
          (swap! c inc)
          (println "delete " id url)
          (ignore-error
           (with-h2
             (delete-rows :crawler_links ["id = ?" id]))))))
    (println "count" @c)))

(defn import-data [{:keys [data-path] :or {data-path "/tmp/rssminer_data"}}]
  (let [{:keys [feeds links rss]} (read-string (slurp data-path))]
    (doseq [l links]
      (ignore-error (with-h2 (insert-record :crawler_links l))))
    (doseq [r rss]
      (ignore-error (with-h2 (insert-record :rss_links r))))
    (doseq [feed feeds]
      (with-h2 (ignore-error
                (insert-record :feeds feed))))))

(defn calculate-frenquency [{:keys [limit size] :or {limit 5000 size 50}}]
  (let [split #(str/split % #"\W")
        fren (frequencies
              (flatten
               (map #(-> % :url URI. .getHost split)
                    (fetch-rss-links limit))))
        want (take size (reverse (sort-by val fren)))]
    (clojure.pprint/pprint want)))

(defn -main [& args]
  "rssminer admin"
  (let [[options _ banner]
        (cli args
             ["-c" "--command"
              "init-db, clean-rss, clean-links, rebuild-index, cal"
              :parse-fn keyword]
             ["-p" "--password" "password" :default "123456"]
             ["--db-path" "H2 Database path" :default "/dev/shm/rssminer"]
             ["--data-path" "Backup, restore data path"
              :default "/tmp/rssminer"]
             ["--index-path" "Path to store lucene index"
              :default "/dev/shm/index"]
             ["--limit" "how much to calculate" :default 5000
              :parse-fn to-int]
             ["--size" "size to print" :default 50 :parse-fn to-int]
             ["--[no-]help" "Print this help"])]
    (when (:help options) (println banner) (System/exit 0))
    (if (= :init-db (:command options))
      (setup-db options)
      (do
        (use-h2-database! (:db-path options))
        (case (:command options)
          :clean-rss
          (clean-rss-links)
          :clean-links
          (clean-crawler-links)
          :rebuild-index
          (do (use-index-writer! (:index-path options))
              (rebuild-index))
          :cal
          (calculate-frenquency options))))))

