(ns setup-database
  (:use (freader [database :only [use-psql-database! close-global-psql-factory]]
                 [routes :only [app]])
        (freader.db [user :only [create-user]]
                    [util :only [get-con exec-stats exec-prepared-sqlfile]])
        [sandbar.stateful-session :only [session-get]]
        [clojure.contrib.json :only [json-str]]))

(def links ["http://blog.raek.se/feed/"
            "http://blog.raynes.me/?feed=rss2"
            "http://blog.sina.com.cn/rss/kaifulee.xml"
            "http://cemerick.com/feed/"
            "http://data-sorcery.org/feed/"
            "http://norvig.com/rss-feed.xml"
            "http://planet.clojure.in/atom.xml"
            "http://techbehindtech.com/feed/"
            "http://weblogs.asp.net/scottgu/atom.aspx"
            "http://weblogs.asp.net/scottgu/rss.aspx"
            "http://www.alistapart.com/rss.xml"
            "http://www.asp.net/learn/videos/rss.ashx"
            "http://www.ibm.com/developerworks/views/java/rss/libraryview.jsp"
            "http://www.ubuntugeek.com/feed/"])

(defn setup []
  (let [db-name "freader"]
    (with-open [con (get-con "postgres")]
      (exec-stats con
                  (str "DROP DATABASE IF EXISTS " db-name)
                  (str "CREATE DATABASE " db-name)))
    (exec-prepared-sqlfile db-name)
    (use-psql-database! db-name)
    (let [user (create-user {:name "feng"
                             :password "123456"
                             :email "shenedu@gmail.com"})]
      (binding [session-get (fn [arg]
                              (if (= arg :user) user arg))]
        (doseq [link links]
          (apply (app) [{:uri "/api/subscription"
                         :request-method :post
                         :body (json-str {:link link})}]))))
    (close-global-psql-factory)))

(setup)
