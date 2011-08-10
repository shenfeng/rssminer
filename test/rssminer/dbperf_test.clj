(ns rssminer.dbperf-test
  (:use (rssminer [database :only [import-h2-schema! use-h2-database!]])
        [rssminer.db.util :only [h2-query]]
        [clojure.tools.logging :only [info]])
  (:require [clojure.string :as str]
            [rssminer.http :as http]
            [rssminer.db.crawler :as db]
            [rssminer.time :as time]))

(def lines (str/split (slurp "test/test-rss.xml") #"\n"))
(def words (filter (complement str/blank?)
                   (str/split (slurp "test/test-rss.xml") #"\W")))

(defn gen-rss-links []
  (map (fn [line url]
         {:url url
          :title line
          :domain (http/extract-host url)
          :next_check_ts (+ (rand-int 3000)
                            (time/now-seconds))})
       (cycle lines)
       (map (fn [[a b c d]] (str "http://" a "." b "." (rand-int 50)
                                ".com/" c "/" d))
            (partition 4 (cycle words)))))

(defn do-insert [& {:keys [n]}]
  (.delete (java.io.File. "/tmp/h2_perf.trace.db"))
  (.delete (java.io.File. "/tmp/h2_perf.h2.db"))
  (use-h2-database! "/tmp/h2_perf")
  ;; (use-h2-database!
  ;;  "/tmp/h2_perf;TRACE_LEVEL_FILE=2;TRACE_MAX_FILE_SIZE=1000")
  (import-h2-schema!)
  (let [refer (first (gen-rss-links))
        n (or n 100)]
    (doseq [rss (partition 10 (take n (gen-rss-links)))]
      (db/insert-crawler-links refer rss))))

(defmacro my-time [expr]
  `(let [start# (System/currentTimeMillis)
         ret# ~expr]
     {:time (- (System/currentTimeMillis) start#)
      :result ret#}))

(defn benchmark []
  (doseq [n (take 3 (iterate (fn [n] (* n 5)) 1000))]
    (let [r (my-time (do-insert :n n))]
      (info "time" (str (:time r) "ms")
            n "insert"
            (-> ["select count (*) as count from crawler_links"] h2-query
                first :count)))
    (dotimes [i 5]
      (let [c (+ (* i 10) 80)
            r (my-time (db/fetch-crawler-links c))]
        (info  "time" (str (:time r) "ms") c
               "fetched"
               (count (:result r)))))
    (info "\n")))
