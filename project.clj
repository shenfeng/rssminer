(defproject rssminer "1.0.0"
  :description "A feed reader written in clojure"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/data.json "0.1.1"]
                 [org.clojure/java.jdbc "0.0.6"]
                 [org.clojure/tools.cli "0.1.0"]
                 [org.clojure/tools.logging "0.2.0"]
                 [com.h2database/h2 "1.3.159"]
                 [log4j/log4j "1.2.16"]
                 [javax.mail/mail "1.4.4"]
                 [org.apache.commons/commons-email "1.2"]
                 [compojure "0.6.5"]
                 [enlive "1.0.0"]
                 [org.apache.lucene/lucene-core "3.3.0"]
                 [net.java.dev.rome/rome "1.0.0"]
                 [ring/ring-core "0.3.11"]
                 [me.shenfeng/ring-netty-adapter "0.0.1-SNAPSHOT"]]
  :dev-resources-path "/usr/lib/jvm/java-6-openjdk/lib/tools.jar:/usr/lib/jvm/java-6-openjdk/src.zip"
  :exclusions [javax.activation/activation]
  :repositories {"java.net" "http://download.java.net/maven/2/"}
  :warn-on-reflection true
  :aot [rssminer.main]
  :jvm-opts ["-XX:+UseCompressedOops"
             "-XX:+TieredCompilation"
             "-XX:+UseCompressedStrings"]
  ;; :jvm-opts ["-agentlib:hprof=cpu=samples,format=b,file=/tmp/profile.txt"]
  ;; :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n"]
  :dev-dependencies [[clojure-source "1.2.1"]
                     [swank-clojure "1.4.0-SNAPSHOT"]])
