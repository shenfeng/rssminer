(defproject rssminer "1.0.0"
  :description "A feed reader written in clojure"
  :dependencies [[clojure "1.3.0"]
                 [org.clojure/data.json "0.1.2"]
                 [org.clojure/java.jdbc "0.1.0"]
                 [org.clojure/tools.cli "0.1.0"]
                 [org.clojure/tools.logging "0.2.3"]
                 [com.h2database/h2 "1.3.161"]
                 [ch.qos.logback/logback-classic "0.9.30"]
                 [javax.mail/mail "1.4.4"]
                 [org.apache.commons/commons-email "1.2"]
                 [compojure "0.6.5"]
                 [enlive "1.0.0"]
                 [commons-io "2.1"]
                 [commons-codec "1.5"]
                 [org.apache.lucene/lucene-core "3.4.0"]
                 [org.apache.lucene/lucene-queries "3.4.0"]
                 [org.apache.lucene/lucene-analyzers "3.4.0"]
                 [net.java.dev.rome/rome "1.0.0"]
                 [ring/ring-core "1.0.0-RC1"]
                 [me.shenfeng/netty-http "1.0.0-SNAPSHOT"]
                 [me.shenfeng/ring-netty-adapter "0.0.1-SNAPSHOT"]]
  :dev-resources-path "/usr/lib/jvm/java-6-sun/lib/tools.jar:/usr/lib/jvm/java-6-sun/lib/src.zip"
  :exclusions [javax.activation/activation]
  :repositories {"java.net" {:url "http://download.java.net/maven/2/"
                             :snapshots false}}
  :warn-on-reflection true
  :javac-options {:source "1.6" :target "1.6" :debug "true" :fork "true"}
  :java-source-path "src/java"
  :aot [rssminer.main]
  :jvm-opts ["-XX:+UseCompressedOops"
             "-Dsun.net.inetaddr.ttl=0"
             "-Djava.net.preferIPv4Stack=true"
             "-XX:+TieredCompilation"
             "-XX:-DisableExplicitGC"
             "-XX:+UseCompressedStrings"
             "-XX:-UseLoopPredicate"
             "-Xmx512m"
             "-Xms512m"]
  ;; :jvm-opts ["-agentlib:hprof=cpu=samples,format=b,file=/tmp/profile.txt"]
  ;; :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n"]
  :dev-dependencies [[swank-clojure "1.4.0-SNAPSHOT"]
                     [junit/junit "4.8.2"]
                     [com.google.guava/guava "10.0"]
                     [org.apache.lucene/lucene-spellchecker "3.4.0"]])
