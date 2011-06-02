(ns setup-database
  (:use (feng.rss [test-util :only [TEST_DB_HOST TEST_PSQL_PASSWORD
                                    TEST_PSQL_USERNAME exec-prepared-sqlfile]]
                  [test-common :only [test-user]]
                  [database :only [use-psql-database! close-global-psql-factory]]
                  [test-common :only [mock-http-get test-user auth-app]])
        [feng.rss.db.user :only [create-user]]
        clojure.contrib.json))

(def links ["http://weblogs.asp.net/scottgu/rss.aspx"
            "http://blog.sina.com.cn/rss/kaifulee.xml"
            "http://norvig.com/rss-feed.xml"
            "http://www.asp.net/learn/videos/rss.ashx"
            "http://blog.raek.se/feed/"
            "http://cemerick.com/feed/"
            "http://blog.raynes.me/?feed=rss2"
            "http://weblogs.asp.net/scottgu/atom.aspx"])

(defn setup []
  (let [db-name "feedreader"
        drop-sql (str "DROP DATABASE IF EXISTS " db-name)
        create-sql (str "CREATE DATABASE " db-name)
        con-uri (str "jdbc:postgresql://" TEST_DB_HOST "/postgres")
        con (java.sql.DriverManager/getConnection
             con-uri TEST_PSQL_USERNAME TEST_PSQL_PASSWORD)]
    (.close (doto (.createStatement con)
              (.addBatch drop-sql)
              (.addBatch create-sql)
              (.executeBatch)))
    (use-psql-database! :jdbc-url (str "jdbc:postgresql://"
                                       TEST_DB_HOST "/" db-name )
                        :user TEST_PSQL_USERNAME
                        :password TEST_PSQL_PASSWORD)
    (exec-prepared-sqlfile db-name)
    (create-user test-user)
    (doseq [link links]
      (auth-app {:uri "/api/feedsource"
                 :request-method :put
                 :body (json-str {:link link})}))
    (close-global-psql-factory)
    (.close con)))

(setup)
