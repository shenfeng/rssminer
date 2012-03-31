(defproject rssminer "1.0.0"
  :description "A feed reader written in clojure"
  :dependencies [[clojure "1.3.0"]
                 [org.clojure/data.json "0.1.2"]
                 [org.clojure/java.jdbc "0.1.1"]
                 [org.clojure/tools.cli "0.2.1"]
                 [org.clojure/tools.logging "0.2.3"]
                 [ch.qos.logback/logback-classic "0.9.30"]
                 [javax.mail/mail "1.4.4"]
                 [org.apache.commons/commons-email "1.2"]
                 [compojure "1.0.1"]
                 [enlive "1.0.0"]
                 [commons-io "2.1"]
                 [redis.clients/jedis "2.0.0"]
                 [commons-codec "1.5"]
                 [mysql/mysql-connector-java "5.1.18"]
                 [org.apache.lucene/lucene-core "3.5.0"]
                 [org.apache.lucene/lucene-queries "3.5.0"]
                 [org.apache.lucene/lucene-analyzers "3.5.0"]
                 [net.java.dev.rome/rome "1.0.0"]
                 [ring/ring-core "1.0.1"]
                 [me.shenfeng/async-http-client "1.0.3"]
                 [me.shenfeng/async-ring-adapter "1.0.1"]]
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
  :dev-dependencies [[swank-clojure "1.4.0"]
                     [junit/junit "4.8.2"]
                     [commons-lang "2.3"]
                     [com.google.guava/guava "10.0"]
                     [org.apache.lucene/lucene-spellchecker "3.4.0"]])
