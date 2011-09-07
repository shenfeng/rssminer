(defproject rssminer "1.0.0"
  :description "A feed reader written in clojure"
  :dependencies [[clojure "1.2.1"]
                 [org.clojure/data.json "0.1.1"]
                 [org.clojure/java.jdbc "0.0.6"]
                 [org.clojure/tools.cli "0.1.0"]
                 [org.clojure/tools.logging "0.2.0"]
                 [com.h2database/h2 "1.3.159"]
                 [ch.qos.logback/logback-classic "0.9.29"]
                 [javax.mail/mail "1.4.4"]
                 [org.apache.commons/commons-email "1.2"]
                 [compojure "0.6.5"]
                 [org.apache.lucene/lucene-core "3.3.0"]
                 [org.apache.lucene/lucene-queries "3.3.0"]
                 [org.apache.lucene/lucene-analyzers "3.3.0"]
                 [net.java.dev.rome/rome "1.0.0"]
                 [ring/ring-core "0.3.11"]
                 [me.shenfeng/netty-http "1.0.0-SNAPSHOT"]
                 [me.shenfeng/enlive "1.2.0-SNAPSHOT"]
                 [me.shenfeng/ring-netty-adapter "0.0.1-SNAPSHOT"]]
  :dev-resources-path "/usr/lib/jvm/jdk1.7.0/lib/tools.jar:/usr/lib/jvm/jdk1.7.0/src.zip"
  :exclusions [javax.activation/activation]
  :repositories {"java.net" "http://download.java.net/maven/2/"}
  :warn-on-reflection true
  :javac-options {:source "1.6" :target "1.6"}
  :java-source-path "src/java"
  :aot [rssminer.main]
  :jvm-opts ["-XX:+UseCompressedOops"
             "-XX:+TieredCompilation"
             "-XX:+UseCompressedStrings"
             "-XX:-UseLoopPredicate"
             "-Xmx320m"
             "-Xms192m"]
  ;; :jvm-opts ["-agentlib:hprof=cpu=samples,format=b,file=/tmp/profile.txt"]
  ;; :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n"]
  :dev-dependencies [[swank-clojure "1.4.0-SNAPSHOT"]
                     [junit/junit "4.8.2"]
                     [org.apache.lucene/lucene-spellchecker "3.3.0"]])
