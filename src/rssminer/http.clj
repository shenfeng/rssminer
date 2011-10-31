(ns rssminer.http
  (:use [clojure.tools.logging :only [info error debug trace fatal]]
        [rssminer.util :only [assoc-if ignore-error]])
  (:refer-clojure :exclude [get])
  (:require [clojure.string :as str]
            [rssminer.config :as conf])
  (:import java.net.URI
           [rssminer Utils$Info Utils$Pair Links Links$LinksConf]
           me.shenfeng.Utils
           [me.shenfeng.http HttpClient HttpClientConfig]
           org.jboss.netty.handler.codec.http.HttpResponse
           org.apache.commons.codec.binary.Base64))

(def ^Links links
  (Links. (doto (Links$LinksConf.)
            (.setIgnoredExtensions conf/ignored-url-extensions)
            (.setBadDomainPattens conf/bad-domain-pattens)
            (.setBlackDomainStr conf/black-domain-strs)
            (.setAcceptedTopDomains conf/accepted-top-domains)
            (.setBadRssTitlePattens conf/bad-rss-title-pattens)
            (.setBadRssUrlPattens conf/bad-rss-url-pattens))))

(defonce ^{:tag HttpClient}
  client (HttpClient. (doto (HttpClientConfig.)
                        (.setWorkerThread 1) ; 1 is ok
                        (.setAcceptedContentTypes '("text" "xml"))
                        (.setUserAgent conf/rssminer-agent)
                        (.setMaxLength (* 512 1024))   ; 512k
                        (.setMaxChunkSize (* 32 1024)) ; tricky
                        (.setTimerInterval 4000) ; 4s check timeout
                        (.setConnectionTimeOutInMs 12000) ;12s
                        (.setRequestTimeoutInMs 40000)    ;40s
                        (.setReceiveBuffer (* 32 1024))   ;32k
                        (.setSendBuffer (* 8 1024)))))    ;8k

(defn ^URI clean-resolve [base part]
  (.resoveAndClean links base part))

(defn extract-host [^String host]
  (let [^URI uri (URI. host)
        port (if (= -1 (.getPort uri)) ""
                 (str ":" (.getPort uri)))
        schema (.getScheme uri)
        host (.getHost uri)]
    (str schema "://" host port)))

(defn parse-response [^HttpResponse response]
  (let [status (-> response .getStatus .getCode)
        names (.getHeaderNames response)]
    {:status status
     :headers (reduce #(assoc %1 (-> %2 str/lower-case keyword)
                              (.getHeader response %2)) {} names)
     :body (when (= 200 status) (Utils/bodyStr response))}))

(defn ^{:dynamic true} get [url & {:keys [last-modified]}]
  (let [headers (if last-modified {"If-Modified-Since" last-modified} {})]
    (parse-response (.get (.execGet client (URI. url) headers)))))

(defn ^{:dynamic true} download-rss  [url]
  (try
    (update-in (get url) [:body]        ;convert to string
               (fn [in] (when in (slurp in))))
    (catch Exception e
      (error e "download-rss" url))))

(defn extract-links [base html]
  (let [^Utils$Info info (rssminer.Utils/extract html base links)]
    {:rss (map (fn [^Utils$Pair r]
                 {:title (.title r)
                  :url (.url r)}) (.rssLinks info))
     :links (map (fn [^URI uri]
                   {:url (str uri)
                    :domain (.getHost uri)}) (.links info))
     :title (.title info)}))
