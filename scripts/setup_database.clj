(ns setup-database
  (:use (freader [test-util :only [exec-prepared-sqlfile get-con exec-stats]]
                  [test-common :only [test-user]]
                  [database :only [use-psql-database! close-global-psql-factory]]
                  [test-common :only [mock-http-get test-user auth-app]])
        [freader.db.user :only [create-user]]
        clojure.contrib.json))

(def links ["http://weblogs.asp.net/scottgu/rss.aspx"
            "http://blog.sina.com.cn/rss/kaifulee.xml"
            "http://norvig.com/rss-feed.xml"
            "http://www.asp.net/learn/videos/rss.ashx"
            "http://blog.raek.se/feed/"
            "http://techbehindtech.com/feed/"
            "http://cemerick.com/feed/"
            "http://blog.raynes.me/?feed=rss2"
            "http://weblogs.asp.net/scottgu/atom.aspx"])

(defn setup []
  (let [db-name "feedreader"]
    (with-open [con (get-con "postgres")]
      (exec-stats con
                  (str "DROP DATABASE IF EXISTS " db-name)
                  (str "CREATE DATABASE " db-name)))
    (exec-prepared-sqlfile db-name)
    (use-psql-database! db-name)
    (create-user test-user)
    (doseq [link links]
      (auth-app {:uri "/api/feeds"
                 :request-method :put
                 :body (json-str {:link link})}))
    (close-global-psql-factory)))

(setup)
