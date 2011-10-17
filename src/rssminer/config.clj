(ns rssminer.config
  (:import [java.net Proxy Proxy$Type InetSocketAddress]))

(defonce env-profile (atom :dev))

(defn in-prod? [] (= @env-profile :prod))

(defn in-dev? [] (= @env-profile :dev))

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

(def socks-proxy (Proxy. Proxy$Type/SOCKS
                         (InetSocketAddress. "127.0.0.1" 3128)))

(def no-proxy Proxy/NO_PROXY)

(def rssminer-agent
  "Mozilla/5.0 (compatible; Rssminer/1.0; +http://rssminer.net)")

(def ungroup "ungrouped")

(def crawler-queue 200)

(def fetcher-queue 100)

(def dns-prefetch true)

(def fetch-size 100)

(def ignored-url-extensions
  '("jpg" "png" "gif" "css" "js" "jpeg"
    "pdf" "doc" "wma" "exe" "jar" "mp3"
    "swf" "mp4" "wmv" "flv" "rm" "mov"
    "zip" "mkv" "rar" "apk"))

(def accepted-top-domains '("com" "net" "me" "cn" "org"))

(def bad-domain-pattens '(#"\d{3,}"))

(def bad-rss-title-pattens '(#"(?i)comment"))

(def bad-rss-url-pattens '(#"(?i)\bcomment"))

(def black-domain-strs
  '( ;; no value?
    "weatheradd." "txooo." "dqccc." "jaiku."
    "informer." "tumblr." "skyrock."
    "deviantart." "duowan." "dushifang." "daportfolio."
    "newsvine." "5d6d." "kohit." "lyrics." "fileflash."
    "pinkbike." "buzznet." "proboards." "restorm." "9che."
    "soxsok." "insanejournal." "filehungry." "malavida."
    "smartcode." "taotaocar" "sourceforge." "minitokyo."
    "easyrpg." "shufa." "keduo." "gov." "artician."
    "stonebuy." "pissedconsumer" "clickbank"

    "superpokepets." "deadjournal." "dreamwidth." "sheezyart."
    "nuclearscripts." "acidfiles." "fyxm." "stonebuy."
    "51daifu." "youboy." "cntrades."

    ;; file download
    "gratis." "apponic."
    ;; glogster

    "tuchong." "wikia."

    "animepaper." "typepad." ;;nice UI

    "bbs." "wap." "news." "forum" "sports." "shop." "taobao"
    "video." "alibaba." "mail." "convert" "game" "download"
    "google" "flash" "community"

    ;; intresting, but not for rss
    "meetup.c"

    ;; unknow language
    "mihanblog." "blogfa." "xanga." "blogsky." "fotopages."
    "loxblog." "geschichten." "kostenlos." "artelista."
    "parsiblog." "blogcu." "slmame." "exteen." "paginasamarillas."
    "foroactivo." "hispavista." "promodj." "vuodatus." "seesaa."
    "dtiblog." "fc2." "tistory." "eklablog." "monbebeblog."
    "metroblog." "pixnet." "nifty." "cocolog" "bloxode"

    "polyvore" "over-blog" "backpage"

    "linkinpark" "soufun" "house" "canalblog" "livejournal"
    ;; "blshe", "hubpages"

    ;; sex
    "adult" "cam" "pussy" "joyfeeds" "sex" "girl" "fuck"
    "horny" "naughty" "penetrationista" "suckmehere"
    "kontakt" "bilder" "dicke" "swinger" "1euro" "1buck"
    "thumblogger" "usrealitysites" "swinger" "mature" "xxx"
    "erotik" "willig" "porn"

    ;; su tao wang. many sub domain, but useless
    "niniweblog" "china56ecn" "centerblog" "heshengtang"
    "wayongroup" "pharmavantage" "zhangxun" "broadchemical"
    "ittong" "cotion" "inforice" "suneternal" "jiudaplc" "suanti"
    "synua" "cetc34" "czlyyy"))

(defn reseted-url? [url]
  (some #(re-find % url) [#"blogspot\.com"]))

(def popular-tags ["clojure" "compojure" "jquery" "jdk" "linux"
                   "database" "performance"  "java" "emacs"
                   "web" "python" "vim"])

(defn multi-domain? [domain]
  (#{"blogs.oracle.com"} domain))
