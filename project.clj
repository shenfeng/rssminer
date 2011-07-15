(defproject freader "1.0.0"
  :description "A feed reader written in clojure"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure.contrib/json "1.3.0-alpha4"]
                 [org.clojure.contrib/sql "1.3.0-alpha4"]
                 [org.clojure.contrib/command-line "1.3.0-alpha4"]
                 [commons-codec/commons-codec "1.5"]
                 [commons-dbcp/commons-dbcp "1.4"]
                 [commons-io/commons-io "2.0.1"]
                 [compojure "0.6.4"]
                 [enlive "1.0.0"]
                 [org.apache.lucene/lucene-core "3.2.0"]
                 [net.java.dev.rome/rome "1.0.0"]
                 [postgresql/postgresql "9.0-801.jdbc4"]
                 [ring/ring-core "0.3.10"]
                 [org.signaut/ring-jetty7-adapter "0.3.10"]]
  :dev-resources-path "/usr/lib/jvm/java-6-openjdk/lib/tools.jar:/usr/lib/jvm/java-6-openjdk/src.zip"
  :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n"]
  :dev-dependencies [[swank-clojure "1.4.0-SNAPSHOT"]
                     [clojure-source "1.2.1"]
                     [org.clojure.contrib/mock "1.3.0-alpha4"]
                     [org.clojure.contrib/trace "1.3.0-alpha4"]])
