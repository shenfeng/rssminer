(ns rssminer.http
  (:use [clojure.tools.logging :only [info error debug trace fatal]]
        [rssminer.util :only [assoc-if]])
  (:refer-clojure :exclude [get])
  (:require [clojure.string :as str]
            [rssminer.config :as conf]
            [net.cgrand.enlive-html :as html])
  (:import [java.net URI URL HttpURLConnection SocketException
            ConnectException UnknownHostException SocketTimeoutException]
           [java.util.zip InflaterInputStream GZIPInputStream]
           [java.io InputStream StringReader]
           [me.shenfeng.http Utils HttpClient]
           org.jboss.netty.handler.codec.http.HttpResponse
           org.apache.commons.io.IOUtils
           org.apache.commons.codec.binary.Base64))

(defn extract-host [^String host]
  (let [^URI uri (URI. host)
        port (if (= -1 (.getPort uri)) ""
                 (str ":" (.getPort uri)))
        schema (.getScheme uri)
        host (.getHost uri)]
    (str schema "://" host port)))

(defn clean-url [^String host]
  (when host
    (let [path (-> host URI. .getRawPath)
          host (extract-host host)
          url (str host path)]
      (when-not (or (conf/black-domain? host)
                    (re-find conf/ignored-url-patten url))
        url))))

(defonce client (me.shenfeng.http.HttpClient.))

(defn parse-response [^HttpResponse response]
  (let [status (-> response .getStatus .getCode)
        names (.getHeaderNames response)]
    {:status status
     :headers (reduce #(assoc %1 (-> %2 str/lower-case keyword)
                              (.getHeader response %2)) {} names)
     :body (when (= 200 status) (Utils/bodyString response))}))

(defn resolve-url [base link]
  (try
    (when-not (or (str/blank? link)
                  (re-find conf/non-url-patten link))
      (let [base (URI. (if (= base (extract-host base))
                         (str base "/")
                         base))
            link (URI. (str/trim link))]
        (str/trim (str (.resolve base link)))))
    (catch Exception e)))

(defn reset-e?
  "Is the given SocketException is caused by connection reset"
  [^SocketException e]
  (= "Connection reset" (.getMessage e)))

(defn request [{:keys [url headers proxy?]}]
  (let [proxy (if proxy? conf/socks-proxy conf/no-proxy)
        ^HttpURLConnection
        con (doto (.. (URL. url) (openConnection proxy))
              (.setReadTimeout 3500)
              (.setConnectTimeout 3500))]
    (.setInstanceFollowRedirects con false)
    (doseq [header headers]
      (.setRequestProperty con (name (key header)) (val header)))
    ;; 0 is status line
    (let [resp-headers (loop [i 1 headers {}]
                         (let [k (.getHeaderFieldKey con i)
                               v (.getHeaderField con i)]
                           (if k
                             (recur (inc i)
                                    (assoc headers
                                      (keyword (str/lower-case k)) v))
                             headers)))
          status (.getResponseCode con)]
      {:status status
       :headers resp-headers
       :body (when (= status 200) (.getInputStream con))})))

(defn wrap-decompression [client]
  (fn [req]
    (let [resp (client req)]
      (case (get-in resp [:headers :content-encoding])
        "gzip"
        (update-in resp [:body]
                   (fn [in]
                     (when in (GZIPInputStream. in))))
        "deflate"
        (update-in resp [:body]
                   (fn [in]
                     (when in (InflaterInputStream. in))))
        resp))))

(defn wrap-redirect [client]
  (fn [req]
    (let [resp (client req)]
      (if (#{301 302 307} (:status resp))
        (client (assoc req :url (get-in resp [:headers :location])))
        resp))))

(defn wrap-proxy [client]
  (fn [{:keys [url] :as req}]
    (if (conf/reseted-url? url)
      (client (assoc req :proxy? true))
      (try
        (client req)
        (catch SocketException e
          (if (and (reset-e? e) (not (:proxy? req)))
            (do
              (info url "is reseted")
              (client (assoc req :proxy? true)))
            (throw e)))))))

(defn wrap-exception [client]
  (fn [req]
    (try (client req)
         (catch ConnectException _
           {:status 450
            :headers {}})
         (catch UnknownHostException _
           {:status 451
            :headers {}})
         (catch SocketTimeoutException _
           {:status 460
            :headers {}})
         (catch Exception e
           (error (:url req) e)
           {:status 452
            :headers {}})
         (catch Error e
           (fatal e (:url req))
           {:status 452
            :headers {}}))))

(def request* (-> request
                  wrap-decompression
                  wrap-proxy
                  wrap-redirect
                  wrap-exception))

(defn ^{:dynamic true} get
  [url & {:keys [last-modified]}]
  (request* {:url url
             :headers (assoc-if {:Connection "close"
                                 :User-Agent conf/rssminer-agent
                                 :Accept-Encoding "gzip, deflate"}
                                :If-Modified-Since last-modified)}))

(defn ^{:dynamic true} download-favicon [url]
  (let [icon-url (str (extract-host url) "/favicon.ico")]
    (try
      (let [{:keys [body Content-Type]} (get icon-url)
            img (when body (Base64/encodeBase64String
                            (IOUtils/toByteArray ^InputStream body)))
            code (if Content-Type
                   (str "data:" Content-Type ";base64,")
                   "data:image/x-icon;base64,")]
        (when img (StringReader. (str code img))))
      (catch Exception e
        (error e "download-favicon" icon-url)))))

(defn ^{:dynamic true} download-rss  [url]
  (try
    (update-in (get url) [:body]        ;convert to string
               (fn [in] (when in (slurp in))))
    (catch Exception e
      (error e "download-rss" url))))

(defn extract-links [base html]
  (let [resource (html/html-resource (StringReader. html))
        links (html/select resource [:a])
        f #(when-let [url (-> (resolve-url base (-> % :attrs :href))
                              clean-url)]
             {:url url
              :title (html/text %)
              :domain (extract-host url)})]
    {:rss (map (fn [i]
                 {:title (-> i :attrs :title)
                  :url (resolve-url base (-> i :attrs :href))})
               (html/select resource
                            [(html/attr= :type "application/rss+xml")]))
     :links (doall (filter identity (map f links)))}))
