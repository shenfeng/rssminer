(defproject freader "1.0.0"
  :description "A feed reader written in clojure"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/data.json "0.1.1"]
                 [org.clojure/java.jdbc "0.0.5"]
                 [org.clojure/tools.cli "0.1.0"]
                 [commons-codec/commons-codec "1.5"]
                 [com.h2database/h2 "1.3.158"]
                 [org.clojars.shenfeng/clojureql "1.1.0-SNAPSHOT"]
                 [commons-dbcp/commons-dbcp "1.4"]
                 [commons-io/commons-io "2.0.1"]
                 [javax.mail/mail "1.4.4"]
                 [org.apache.commons/commons-email "1.2"]
                 [compojure "0.6.5"]
                 [enlive "1.0.0"]
                 [org.apache.lucene/lucene-core "3.2.0"]
                 [net.java.dev.rome/rome "1.0.0"]
                 [postgresql/postgresql "9.0-801.jdbc4"]
                 [ring/ring-core "0.3.11"]
                 [org.signaut/ring-jetty7-adapter "0.3.11"]]
  :dev-resources-path "/usr/lib/jvm/java-6-openjdk/lib/tools.jar:/usr/lib/jvm/java-6-openjdk/src.zip"
  :exclusions [javax.activation/activation]
  :repositories {"java.net" "http://download.java.net/maven/2/"}
  ;; :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n"]
  :dev-dependencies [[swank-clojure "1.4.0-SNAPSHOT"]
                     [clojure-source "1.2.1"]
                     [org.clojure.contrib/mock "1.3.0-alpha4"]])
