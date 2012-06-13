(defproject rssminer "1.0.0"
  :description "Rssminer - an intelligent RSS reader"
  :dependencies [[clojure "1.4.0"]
                 [commons-codec "1.5"]
                 [commons-io "2.1"]
                 [compojure "1.0.3"]
                 [enlive "1.0.0"]
                 [me.shenfeng/http-kit "1.0.0-SNAPSHOT"]
                 [me.shenfeng/mmseg "0.0.1"]
                 [me.shenfeng/dbcp "0.0.1"]
                 [mysql/mysql-connector-java "5.1.18"]
                 [net.java.dev.rome/rome "1.0.0"]
                 [org.apache.lucene/lucene-core "3.6.0"]
                 [org.clojure/data.json "0.1.2"]
                 [org.clojure/java.jdbc "0.2.1"]
                 [org.clojure/tools.cli "0.2.1"]
                 [org.apache.lucene/lucene-analyzers "3.6.0"]
                 [ch.qos.logback/logback-classic "1.0.1"]
                 [org.clojure/tools.logging "0.2.3"]
                 [redis.clients/jedis "2.1.0"]
                 [ring/ring-core "1.1.0"]]
  :dev-resources-path "/usr/lib/jvm/java-6-sun/lib/tools.jar:/usr/lib/jvm/java-6-sun/lib/src.zip"
  :exclusions [javax.activation/activation]
  :repositories {"java.net" {:url "http://download.java.net/maven/2/"
                             :snapshots false}}
  :warn-on-reflection true
  :javac-options {:source "1.7" :target "1.7" :debug "true" :fork "true"}
  :java-source-path "src/java"
  :aot [rssminer.main, rssminer.admin]
  :main rssminer.main
  :test-selectors {:default (complement :benchmark)
                   :benchmark :benchmark
                   :all (fn [_] true)}
  :jvm-opts ["-Dclojure.compiler.disable-locals-clearing=true"
             "-Djava.net.preferIPv4Stack=true"
             "-Dsun.net.inetaddr.ttl=0"
             "-XX:+TieredCompilation"
             "-Xms512m"
             "-Xmx512m"]
  ;; :jvm-opts ["-agentlib:hprof=cpu=samples,format=b,file=/tmp/profile.txt"]
  ;; :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n"]
  :dev-dependencies [[swank-clojure "1.4.0"]
                     [junit/junit "4.8.2"]
                     [commons-lang "2.3"]
                     [javax.mail/mail "1.4.4"]
                     [org.apache.commons/commons-email "1.2"]
                     [com.google.guava/guava "11.0.2"]
                     [org.apache.lucene/lucene-spellchecker "3.6.0"]])
