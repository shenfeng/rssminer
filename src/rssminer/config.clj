(ns rssminer.config
  (:import [java.net Proxy Proxy$Type InetSocketAddress]))

(defonce env-profile (atom :dev))

(defn in-prod? []
  (= @env-profile :prod))

(defn in-dev? []
  (= @env-profile :dev))

(def netty-option {"receiveBufferSize" 16384
                   "sendBufferSize" 32768
                   "child.receiveBufferSize" 16384
                   "child.sendBufferSize" 32768
                   "reuseAddress" true
                   "child.reuseAddress" true
                   "tcpNoDelay" true
                   "child.tcpNoDelay" true
                   "child.keepAlive" false
                   "child.connectTimeoutMillis" 4000})

(def http-proxy (Proxy. Proxy$Type/HTTP
                        (InetSocketAddress. "127.0.0.1" 3128)))

(def no-proxy Proxy/NO_PROXY)

(def rssminer-agent
  "Mozilla/5.0 (compatible; Rssminer/1.0; +http://rssminer.net)")

(def ungroup "ungrouped")

(def crawler-queue 300)

(def fetcher-queue 100)

(def dns-prefetch true)

(def fetch-size 100)

(def ignored-url-patten
  (re-pattern (str "(?i)(jpg|png|gif|css|js|jpeg|pdf|doc|wma|exe|jar"
                   "mp3|swf|mp4|wmv|flv|rm|mov|zip|mkv|rar|apk)$")))

(defn black-domain? [host]
  (or (not (re-find #"(com|net|me)$" host))
      (some #(re-find % host)
            [#"\d{3,}"
             #"\.a-\w+.com"
             #"informer\.|typepad\."
             #"over-blog|backpage|https"
             ;; no value?
             #"weatheradd|txooo|dqccc"
             ;; unkown language
             #"mihanblog|blogfa|xanga|blogsky|fotopages"
             #"loxblog|geschichten|kostenlos|artelista"
             #"(?i)parsiblog"

             #"polyvore"

             #"blshe|linkinpark|shop|soufun"
             #"skyrock|tumblr|deviantart|taobao"
             #"news\.|forum|bbs\.|sports\.|wap\."
             #"canalblog|livejournal|blogcu|house"
             ;; sex
             #"adult|live|cam|pussy|joyfeeds|sex|girl|fuck"
             #"horny|naughty|penetrationista|suckmehere|free"
             #"kontakt|bilder|dicke|swinger|1euro|1buck"
             #"thumblogger|usrealitysites|swinger|mature|xxx"
             #"erotik|willig"
             ;; su tao wang
             #"niniweblog|china56ecn|centerblog|heshengtang"
             #"wayongroup|pharmavantage|zhangxun|broadchemical"
             #"ittong|cotion|inforice|suneternal|jiudaplc|suanti"
             #"synua|cetc34|czlyyy"])))

(defn reseted-url? [url]
  (some #(re-find % url) [#"blogspot\.com"]))

(def popular-tags ["clojure" "compojure" "jquery" "jdk" "linux"
                   "database" "performance"  "java" "emacs"
                   "web" "python" "vim"])

(defn multi-domain? [domain]
  (#{"http://blogs.oracle.com"} domain))
