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

(def ^Links link (Links. (doto (Links$LinksConf.)
                           (.setIgnoredExtensions conf/ignored-url-extensions)
                           (.setBadDomainPattens conf/bad-domain-pattens)
                           (.setBlackDomainStr conf/black-domain-strs)
                           (.setAcceptedTopDomains conf/accepted-top-domains))))

(defonce ^{:tag HttpClient}
  client (HttpClient. (doto (HttpClientConfig.)
                        (.setMaxLength (* 1025 512)) ; 512k
                        (.setWorkerThread 1)         ; 1 is ok
                        (.setTimerInterval 3000) ; 3s check timeout
                        (.setUserAgent conf/rssminer-agent)
                        (.setConnectionTimeOutInMs 12000) ;12s
                        (.setRequestTimeoutInMs 30000)    ;30s
                        (.setReceiveBuffer 32768)         ;32k
                        (.setSendBuffer 8192))))          ;8k

(defn ^URI clean-resolve [base part]
  (.resoveAndClean link base part))

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

(defn ^{:dynamic true} download-favicon [url]
  (let [icon-url (str (extract-host url) "/favicon.ico")
        resp ^HttpResponse (.get (.execGet client (URI. icon-url) {}))
        ct (or (.getHeader resp "Content-Type") "image/x-icon")]
    (if (= 200 (-> resp .getStatus .getCode))
      (let [img  (Base64/encodeBase64String
                  (-> resp .getContent .slice .array))]
        (when img (str "data:" ct ";base64," img))))))

(defn ^{:dynamic true} download-rss  [url]
  (try
    (update-in (get url) [:body]        ;convert to string
               (fn [in] (when in (slurp in))))
    (catch Exception e
      (error e "download-rss" url))))

(defn extract-links [base html]
  (let [^Utils$Info info (rssminer.Utils/extractInfo html)
        f #(when-let [url (clean-resolve base %)]
             {:url (str url)
              :domain (.getHost url)})]
    {:rss (map (fn [^Utils$Pair r]
                 {:title (.title r)
                  :url (str (clean-resolve base (.url r)))}) (.rssLinks info))
     :links (filter identity (map f (.links info)))
     :title (.title info)}))
