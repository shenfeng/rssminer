(ns rssminer.test-common
  (:use [rssminer.routes :only [app]]
        [clojure.test :only [join-fixtures]]
        [clojure.java.jdbc :only [print-sql-exception-chain]]
        (rssminer [database :only [import-h2-schema! use-h2-database!]]
                  [search :only [use-index-writer!
                                 close-global-index-writer!]])
        (rssminer.db [user :only [create-user]])
        [sandbar.stateful-session :only [session-get]])
  (:require [clojure.string :as str])
  (:import java.io.File
           java.sql.SQLException))

(def ^{:dynamic true} *user1* nil)
(def ^{:dynamic true} *user2* nil)

(def test-user {:name "feng"
                :password "123456"
                :email "shenedu@gmail.com"})

(def test-user2 {:name "feng"
                 :password "123456"
                 :email "feng@gmail.com"})

(defn user-fixture [test-fn]
  (def *user1* (create-user test-user))
  (def *user2* (create-user test-user2))
  (test-fn))

(def auth-app
  (fn [& args]
    (binding [session-get #(if (=  % :user) *user1*
                               %)]
      (apply (app) args))))

(def auth-app2
  (fn [& args]
    (binding [session-get #(if (=  % :user) *user2*
                               %)]
      (apply (app) args))))

(defn lucene-fixture [test-fn]
  (use-index-writer! :RAM)
  (test-fn)
  (close-global-index-writer!))

(defn h2-fixture [test-fn]
  (let [ file (str "mem:crawler_test")]
    (try
      (use-h2-database! file)
      (import-h2-schema!)
      (test-fn)
      (catch SQLException e
        (print-sql-exception-chain e)
        (throw e)))))

(def app-fixture (join-fixtures [lucene-fixture
                                 h2-fixture
                                 user-fixture]))
(defmacro mocking [var new-f & forms]
  `(let [old# (atom nil)]
     (try
       (alter-var-root ~var (fn [f#]
                              (reset! old# f#)
                              ~new-f))
       ~@forms
       (finally
        (alter-var-root ~var (fn [n#] @old#))))))

(def test-rss-str (slurp "test/test-rss.xml"))

(defn mock-download-rss [& args]
  {:body test-rss-str})

(def test-app
  (app))
