(ns rssminer.test-common
  (:use [rssminer.routes :only [app]]
        [clojure.test :only [join-fixtures]]
        [clojure.java.shell :only [sh]]
        [clojure.data.json :only [json-str]]
        [clojure.java.jdbc :only [print-sql-exception-chain]]
        [rssminer.handlers.subscriptions :only [subscribe]]
        (rssminer [search :only [use-index-writer!
                                 close-global-index-writer!]]
                  [util :only [user-id-from-session now-seconds]]
                  [parser :only [parse-feed]]
                  [redis :only [fetcher-enqueue set-redis-pool!
                                redis-pool fetcher-dequeue]]
                  [config :only [rssminer-conf]])
        (rssminer.db [user :only [create-user]]
                     [feed :only [save-feeds]])

        rssminer.classify)
  (:require [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [rssminer.database :as db])
  (:import java.io.File
           rssminer.Utils
           java.sql.SQLException
           [redis.clients.jedis JedisPool Jedis]))

(def user1 nil)
(def user2 nil)

(def test-user {:name "feng"
                :password "123456"
                :like_score 1.0
                :neutral_score 0
                :email "shenedu@gmail.com"})

(def test-user2 {:name "feng"
                 :password "123456"
                 :like_score 1.0
                 :neutral_score 0
                 :email "feng@gmail.com"})

(defn user-fixture [test-fn]
  (def user1 (create-user test-user))
  (def user2 (create-user test-user2))
  (test-fn))

(defn json-body [body]
  (java.io.ByteArrayInputStream. (.getBytes ^String (json-str body))))

(def auth-app
  (fn [& args]
    (binding [user-id-from-session (fn [req] (:id user1))]
      (apply (app) args))))

(def auth-app2
  (fn [& args]
    (binding [user-id-from-session (fn [req] (:id user2))]
      (apply (app) args))))

(defn mk-feeds-fixtrue [resource]
  (fn [test-fn]
    (let [sub (subscribe "http://link-to-scottgu's rss" (:id user1) nil nil)
          feeds (parse-feed (slurp resource))]
      (save-feeds feeds (:rss_link_id sub)))
    (test-fn)))

(defn lucene-fixture [test-fn]
  (use-index-writer! :RAM)
  (test-fn))

(defn- run-admin [cmd params]
  (apply sh (concat ["./scripts/admin"] params [cmd])))

(defn mysql-fixture [test-fn]
  (let [test-db-name "rssminer_test"
        test-user "feng_test"]
    (try
      (run-admin "init-db" ["-d" test-db-name "-u" test-user])
      (db/use-mysql-database! (str "jdbc:mysql://localhost/" test-db-name)
                              test-user)
      (test-fn)
      (catch SQLException e
        (print-sql-exception-chain e)
        (throw e))
      ;; (finally (run-admin "drop-db" ["-d" test-db-name]))
      )))

(defn redis-fixture [test-fn]
  (sh "redis-cli" "flushdb")            ; just clean all
  (set-redis-pool! "127.0.0.1")
  (swap! rssminer-conf assoc :events-threshold (int 3))
  (test-fn)
  (sh "redis-cli" "flushdb"))

(defn- wait-redis-key-present [key]
  (let [^JedisPool client @redis-pool
        ^Jedis j (.getResource client)]
    (try
      (loop [i 20]
        (when-not (.exists j key)
          (Thread/sleep i)
          (recur (+ 20 i))))
      (finally (.returnResource client j)))))

(defn feeds-score-fixture [test-fn]
  (start-classify-daemon)
  (let [subid (:rss_link_id (subscribe "http://test-rss-ink"
                                       (:id user1) nil nil))
        feeds (update-in (parse-feed (slurp "test/atom.xml"))
                         [:entries]
                         (fn [entries]
                           (map (fn [f] (assoc f :published_ts (now-seconds)))
                                entries)))]
    ;; will add to lucene index
    (save-feeds feeds subid)
    (let [ids (map :id (db/mysql-query ["select id from feeds"]))]
      (doseq [id (take 8 ids)]
        (auth-app {:request-method :post
                   :uri (str "/api/feeds/" id "/vote")
                   :body (json-body {:vote (if (even? id) 1 -1)})})))
    (wait-redis-key-present (Utils/genKey (:id user1) subid)))
  (test-fn)
  (stop-classify-daemon))

(defmacro mocking [var new-f & forms]
  `(let [old# (atom nil)]
     (try
       (alter-var-root ~var (fn [f#]
                              (reset! old# f#)
                              ~new-f))
       ~@forms
       (finally
        (alter-var-root ~var (fn [n#] @old#))))))

(defn redis-queue-fixture [test-fn]
  (mocking #'fetcher-enqueue (fn [& args])
           (mocking #'fetcher-dequeue (fn [& args])
                    (test-fn))))

(def app-fixture (join-fixtures [mysql-fixture
                                 user-fixture
                                 redis-fixture
                                 lucene-fixture
                                 redis-queue-fixture
                                 feeds-score-fixture]))

(def test-app (app))
