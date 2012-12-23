(ns rssminer.database
  (:use [clojure.java.jdbc :only [with-connection do-commands]]
        [clojure.tools.logging :only [debug info]]
        [rssminer.config :only [rssminer-conf cfg]]
        [clojure.java.jdbc :only [with-connection do-commands
                                  as-identifier
                                  with-query-results insert-record]])
  (:require [clojure.string :as str])
  (:import me.shenfeng.dbcp.PerThreadDataSource
           java.text.SimpleDateFormat
           java.util.Locale
           java.sql.Timestamp))

(defonce mysql-db-factory (atom {}))

(defn- jdbc-params []                   ; default jdbc params
  (apply str (interpose "&" (map (fn [[k v]] (str k "=" v))
                                 {"useLocalSessionState" true
                                  "characterEncoding" "utf8"
                                  "cacheCallableStmts" true
                                  "prepStmtCacheSize" 100 ; default 25
                                  ;; for rebuild index
                                  ;; "useCursorFetch" true
                                  ;; "defaultFetchSize" 400
                                  "cachePrepStmts" true
                                  "useServerPrepStmts" true
                                  "maintainTimeStats" false}))))

(defn do-mysql-commands [& commands]
  (with-connection @mysql-db-factory
    (apply do-commands commands)))

(defn close-global-mysql-factory! []
  (if-let [ds (:ds @mysql-db-factory)]
    (.close ^PerThreadDataSource ds)
    (reset! mysql-db-factory nil)))

(defmacro with-mysql [& body]
  `(with-connection @mysql-db-factory
     ~@body))

(defn mysql-query [query]
  (with-mysql (with-query-results rs query (doall rs))))

(defn mysql-insert [table record]
  (:generated_key (with-mysql (insert-record table record))))

(defn mysql-insert-and-return [table record]
  (let [id (mysql-insert table record)]
    (first (mysql-query [(str "select * from " (name table)
                              " where id = ?") id]))))

(defn parse-timestamp [str]
  (let [f (SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss ZZZ" Locale/US)]
    (Timestamp. (.getTime (.parse f str)))))

(defn use-mysql-database! [& {:keys [url user password]}]
  (let [^String url (or url (cfg :db-url))
        url (if (= -1 (.indexOf url (int \?)))
              (str url "?" (jdbc-params)) (str url "&" (jdbc-params)))
        ds (PerThreadDataSource. url
                                 (or user (cfg :db-user))
                                 (or password (cfg :db-pass)))]
    (reset! mysql-db-factory {:factory (fn [& args] (.getConnection ds))
                              :ds ds})
    ;; init demo-user as soon as possible
    (swap! rssminer-conf assoc
           :data-source ds
           :demo-user (first (mysql-query ["SELECT id, conf, like_score, neutral_score
                  FROM users WHERE email = 'demo@rssminer.net'"])))))
