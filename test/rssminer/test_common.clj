(ns rssminer.test-common
  (:use [rssminer.routes :only [app]]
        [clojure.test :only [join-fixtures]]
        [clojure.java.shell :only [sh]]
        [clojure.data.json :only [json-str]]
        [clojure.java.jdbc :only [print-sql-exception-chain]]
        [rssminer.handlers.subscriptions :only [subscribe]]
        (rssminer [search :only [use-index-writer!
                                 close-global-index-writer!]]
                  [util :only [session-get]]
                  [parser :only [parse-feed]]
                  [redis :only [fetcher-enqueue fetcher-dequeue]]
                  [http :only [download-rss]])
        (rssminer.db [user :only [create-user]]
                     [feed :only [save-feeds]]))
  (:require [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [rssminer.database :as db])
  (:import java.io.File
           java.sql.SQLException))

(def user1 nil)
(def user2 nil)

(def test-user {:name "feng"
                :password "123456"
                :email "shenedu@gmail.com"})

(def test-user2 {:name "feng"
                 :password "123456"
                 :email "feng@gmail.com"})

(defn user-fixture [test-fn]
  (def user1 (create-user test-user))
  (def user2 (create-user test-user2))
  (test-fn))

(def session-keys [:id :email :name :conf])

(def auth-app
  (fn [& args]
    (binding [session-get (fn [req key]
                            (if (= key :user) (select-keys user1 session-keys)
                                (throw (Exception. "session-get error"))))]
      (apply (app) args))))

(def auth-app2
  (fn [& args]
    (binding [session-get (fn [req key]
                            (if (= key :user) (select-keys user2 session-keys)
                                (throw (Exception. "session-get error"))))]
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
      (finally (run-admin "drop-db" ["-d" test-db-name])))))

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

(def app-fixture (join-fixtures [lucene-fixture
                                 mysql-fixture
                                 user-fixture
                                 redis-queue-fixture]))

(def test-app (app))
