(ns freader.test-common
  (:use [freader.routes :only [app]]
        [clojure.test :only [join-fixtures]]
        (freader [database :only [close-global-psql-factory
                                  use-psql-database!]]
                 [search :only [use-index-writer!
                                close-global-index-writer!]])
        (freader.db [util :only [get-con exec-stats exec-prepared-sqlfile]]
                    [user :only [create-user]])
        [sandbar.stateful-session :only [session-get]]))

(defn- gen-random [num]
  (let [alphabet "abcdefghjklmnpqrstuvwxy123456789"]
    (apply str
           (take num (shuffle (seq alphabet))))))

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
  (let [mock-session-get (fn [arg]
                           (if (=  arg :user) *user1*
                               arg))]
    (fn [& args]
      (binding [session-get mock-session-get]
        (apply (app) args)))))

(def auth-app2
  (let [mock-session-get (fn [arg]
                           (if (=  arg :user) *user2*
                               arg))]
    (fn [& args]
      (binding [session-get mock-session-get]
        (apply (app) args)))))

(defn lucene-fixture [test-fn]
  (use-index-writer! :RAM)
  (test-fn)
  (close-global-index-writer!))

(defn postgresql-fixture [test-fn]
  (let [tmpdb (str "test_"  (.toLowerCase (gen-random 3)))]
    (with-open [con (get-con "postgres")]
      (try
        (exec-stats con (str "create database " tmpdb))
        (exec-prepared-sqlfile tmpdb)
        (use-psql-database! tmpdb)
        (test-fn)
        (finally (close-global-psql-factory) ; close global psql connetion
                 (exec-stats con (str "drop database " tmpdb)))))))

(def app-fixture (join-fixtures [lucene-fixture
                                 postgresql-fixture
                                 user-fixture]))

(def test-rss-str (slurp "test/test-rss.xml"))

(defn mock-download-feed-source [& args]
  {:body test-rss-str})

(def test-app
  (app))
