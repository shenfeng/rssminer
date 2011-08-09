(ns rssminer.setup-database
  (:use (rssminer [database :only [use-h2-database! close-global-h2-factory
                                  import-h2-schema!]]
                 [search :only [use-index-writer! close-global-index-writer!]]
                 [routes :only [app]])
        (rssminer.db [user :only [create-user]])
        [sandbar.stateful-session :only [session-get]]
        [clojure.data.json :only [json-str]]))

(def links [
            ;; "http://blog.raek.se/feed/"
            "http://feeds.feedburner.com/ruanyifeng"
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
  (let [db-path "/tmp/rssminer"]
    (use-h2-database! db-path)
    (import-h2-schema!)
    (use-index-writer! "/tmp/feeds-index")
    (let [user (create-user {:name "feng"
                             :password "123456"
                             :email "shenedu@gmail.com"})]
      (binding [session-get #(if (= % :user) user %)]
        (doseq [link links]
          (apply (app) [{:uri "/api/subscriptions/add"
                         :request-method :post
                         :body (json-str {:link link})}]))))
    (close-global-h2-factory)
    (close-global-index-writer!)))
