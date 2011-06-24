(defproject freader "1.0.0"
  :description "A feed reader written in clojure"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure.contrib/json "1.3.0-alpha4"]
                 [org.clojure.contrib/sql "1.3.0-alpha4"]
                 [org.clojure.contrib/command-line "1.3.0-alpha4"]
                 [commons-codec/commons-codec "1.5"]
                 [commons-dbcp/commons-dbcp "1.4"]
                 [commons-io/commons-io "2.0.1"]
                 [compojure "0.6.3"]
                 [enlive "1.0.0"]
                 [net.java.dev.rome/rome "1.0.0"]
                 [postgresql/postgresql "9.0-801.jdbc4"]
                 [ring/ring-core "0.3.8"]
                 [ring/ring-jetty-adapter "0.3.8"]]
  :dev-dependencies [[swank-clojure "1.4.0-SNAPSHOT"]
                     [org.clojure.contrib/mock "1.3.0-alpha4"]
                     [org.clojure.contrib/trace "1.3.0-alpha4"]])
