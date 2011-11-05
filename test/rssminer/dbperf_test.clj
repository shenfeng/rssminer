(ns rssminer.dbperf-test
  (:use (rssminer [database :only [import-h2-schema! use-h2-database!]])
        [clojure.tools.cli :only [cli]]
        [rssminer.util :only [extract-text to-int]]
        [rssminer.test-common]
        [rssminer.db.util :only [h2-query]])
  (:require [clojure.string :as str]
            [rssminer.http :as http]
            [net.cgrand.enlive-html :as html]
            [rssminer.db.crawler :as db]
            [rssminer.time :as time]))

(def lines (str/split (slurp "test/scottgu-atom.xml") #"\n"))
(def words (filter (complement str/blank?)
                   (str/split (slurp "test/scottgu-atom.xml") #"\W")))

(def html (slurp "templates/landing.html"))

(defmacro tick [& body]
  `(let [start# (System/currentTimeMillis)
         r# (do ~@body)]
     {:time (- (System/currentTimeMillis) start#)
      :result r#}))

(defn gen-rss-links []
  (map (fn [line url]
         {:url url
          :title line
          :domain (http/extract-host url)
          :next_check_ts (+ (rand-int 10000)
                            (time/now-seconds))})
       (cycle lines)
       (map (fn [[a b c d]] (str "http://" a "." b "." (rand-int 500)
                                ".com/" c "/" d))
            (partition 4 (cycle words)))))

(defn do-insert [& {:keys [n path]}]
  (.delete (java.io.File. (str path ".h2.db")))
  (use-h2-database! path)
  (import-h2-schema!)
  (let [refer (first (gen-rss-links))]
    (doseq [rss (partition 10 (take n (gen-rss-links)))]
      (db/insert-crawler-links refer rss))))

(defmacro my-time [expr]
  `(let [start# (System/currentTimeMillis)
         ret# ~expr]
     {:time (- (System/currentTimeMillis) start#)
      :result ret#}))

(defn benchmark [{:keys [init times step path]}]
  (doseq [n (take times
                  (iterate (fn [n] (* n step)) init))]
    (let [r (my-time (do-insert :n n :path path))
          inserted (-> ["select count (*) as count from crawler_links"]
                       h2-query first :count)
          time (:time r)]
      (println "\n-----" (java.util.Date.) "-----")
      (println  n "items, inserted" inserted
                (str "in " time "ms,")
                (format "%.2f per ms" (/ (double inserted) time))))
    (dotimes [i 5]
      (let [c (+ (* i 10) 80)
            r (my-time (db/fetch-crawler-links c))]
        (println "candidate"
                 (str (-> ["select count (*) as count from crawler_links
                             where next_check_ts < ?"
                           (time/now-seconds)] h2-query  first :count) ",")
                 "fetched"
                 (count (:result r))
                 (str "in " (:time r) "ms"))))))

(defn main [& args]
  "benchmark database"
  (benchmark
   (first (cli args
               ["-i" "--init" "start" :default 10000 :parse-fn to-int]
               ["-s" "--step" "step" :default 5 :parse-fn to-int]
               ["-c" "--times" "step count":default 3 :parse-fn to-int]
               ["-p" "--path" "tmp db path" :default "/tmp/h2_bench"]))))

(defn sql []
  (str "select id, author, summary, link,guid  from feeds where id in ("
       (str/join ", " (map (fn [& n] (rand-int 673670)) (range 1 15)))
       " )"))

(defn f []  (let [start (System/currentTimeMillis)]
              (h2-query [ (sql) ])
              (let [time (- (System/currentTimeMillis) start)]
                (println (.getName (Thread/currentThread))
                         time "ms")
                time)))

(defn run [n]
  (let [r (doall (apply pcalls (repeat n f)))]
    (println (int (/ (reduce + r) (count r))))))

(defn bench-extract-text []
  (let [n 5000
        times (map (fn [& args] (:time (tick (extract-text html)))) (range n))]
    (println "takes time " (/ (double (reduce + times)) n))))

(defn bench-dns []
  (let [links (rssminer.db.crawler/fetch-crawler-links 50)]
    (time (doseq [url links]
            (try
              (let [host (-> url :url java.net.URI. .getHost)
                    r (my-time (java.net.InetAddress/getAllByName host))]
                (println (:time r) "ms, " (first (:result r))))
              (catch Exception e
                (println "errro")))))))

