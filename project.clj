(defproject freader "1.0.0-SNAPSHOT"
  :description "A feed reader written in clojure"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [compojure "0.6.3"]
                 [enlive "1.0.0"]
                 [clj-http "0.1.3"]
                 [postgresql/postgresql "9.0-801.jdbc4"]
                 [commons-dbcp/commons-dbcp "1.4"]
                 [sandbar/sandbar "0.4.0-SNAPSHOT"]
                 [ring/ring-core "0.3.8"]
                 [ring/ring-jetty-adapter "0.3.8"]
                 [net.java.dev.rome/rome "1.0.0"]]
  :dev-dependencies [[swank-clojure "1.3.1"]])
