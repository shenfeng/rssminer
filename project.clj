(defproject rssminer "2.0.0"
  :description "Rssminer - an intelligent RSS reader"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [commons-codec "1.5"]
                 [commons-io "2.1"]
                 [compojure "1.1.1"]
                 [http-kit "2.2.0-SNAPSHOT"]
                 [me.shenfeng/mustache "1.1"]
                 [me.shenfeng/mmseg "0.0.4"]
                 [me.shenfeng/dbcp "1.0"]
                 [mysql/mysql-connector-java "5.1.21"]
                 [net.java.dev.rome/rome "1.0.0"]
                 [org.apache.lucene/lucene-core "3.6.1"]
                 [org.clojure/data.json "0.1.2"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [org.clojure/tools.cli "0.2.1"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.jsoup/jsoup "1.7.1"]
                 [ch.qos.logback/logback-classic "1.0.1"]
                 [redis.clients/jedis "2.1.0"]
                 [ring/ring-core "1.1.3"]]
  :dev-resources-path "/usr/lib/jvm/java-6-sun/lib/tools.jar:/usr/lib/jvm/java-6-sun/lib/src.zip"
  :exclusions [javax.activation/activation]
  :repositories {"java.net" {:url "http://download.java.net/maven/2/" :snapshots false}}
  :warn-on-reflection true
  :javac-options ["-source" "1.7" "-target" "1.7" "-g" "-encoding" "utf8"]
  :java-source-paths ["src/java"]
  :aot [rssminer.main, rssminer.admin]
  :main rssminer.main
  :uberjar-name "rssminer-standalone.jar"
  :uberjar-exclusions [#".+\.java$" #".+\.sql$" #".+tmpls/.+\.tpl"
                       #".+\.clj$" #"pom.xml"]
  :test-selectors {:default (complement :benchmark)
                   :benchmark :benchmark
                   :all (fn [_] true)}
  :jvm-opts ["-Dclojure.compiler.disable-locals-clearing=true"
             ;; does not work
             ;; "-Dclojure.compiler.elide-meta='[:doc :file :line :added]'"
             "-Djava.net.preferIPv4Stack=true"
             "-Dsun.net.inetaddr.ttl=0"
             "-XX:+TieredCompilation"
             "-Xms512m"
             "-Xmx512m"]
  ;; :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n"]
  :plugins [[lein-swank "1.4.4"]
            [lein-cloverage "1.0.2"]]
  :profiles {:dev {:dependencies [[swank-clojure "1.4.0"]
                                  [junit/junit "4.8.2"]
                                  [commons-lang "2.3"]
                                  [org.ccil.cowan.tagsoup/tagsoup "1.2"]
                                  [org.apache.lucene/lucene-analyzers "3.6.1"]
                                  [org.apache.lucene/lucene-facet "3.6.1"]
                                  [org.apache.lucene/lucene-queries "3.6.1"]
                                  [javax.mail/mail "1.4.4"]
                                  [org.apache.commons/commons-email "1.2"]
                                  [com.google.guava/guava "11.0.2"]
                                  [org.apache.lucene/lucene-spellchecker "3.6.1"]]}})

;; (map meta (vals (ns-publics 'clojure.core)))
;; (require 'rssminer.admin)
;; (map meta (apply concat (map vals (map ns-publics (loaded-libs)))))
