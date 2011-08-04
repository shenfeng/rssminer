(ns freader.http
  (:refer-clojure :exclude [get])
  (:require [clojure.string :as str])
  (:import [java.net URI URL Proxy Proxy$Type InetSocketAddress
            HttpURLConnection SocketException ConnectException
            UnknownHostException]
           [java.util.zip InflaterInputStream GZIPInputStream]))

(def ^{:private true}
  socks-proxy (Proxy. Proxy$Type/SOCKS
                      (InetSocketAddress. "localhost" 3128)))

(def ^{:private true}  no-proxy Proxy/NO_PROXY)

(defn extract-host [^String host]
  (let [^URI uri (URI. host)
        port (if (= -1 (.getPort uri)) ""
                 (str ":" (.getPort uri)))
        schema (.getScheme uri)
        host (.getHost uri)]
    (str schema "://" host port)))

(defonce reseted-hosts (atom #{}))

(defn- reseted-url?
  "If the given url is reseted"
  [url] (@reseted-hosts (extract-host url)))

(defn- add-reseted-url [url]
  (swap! reseted-hosts conj (extract-host url)))

(defn reset-e?
  "Is the given SocketException is caused by connection reset"
  [^SocketException e]
  (= "Connection reset" (.getMessage e)))

(defn request [{:keys [url headers proxy?]}]
  (let [proxy (if proxy? socks-proxy no-proxy)
        ^HttpURLConnection
        con (.. (URL. url) (openConnection proxy))]
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

(defn wrap-redirects [client]
  (fn [req]
    (let [resp (client req)]
      (if (#{301 302 307} (:status resp))
        (let [url (get-in resp [:headers :Location])]
          (client (assoc req :url url)))
        resp))))

(defn wrap-compression [client]
  (fn [req]
    (if (get-in req [:headers :Accept-Encoding])
      (client req)
      (let [resp (client
                  (assoc-in req
                            [:headers :Accept-Encoding] "gzip, deflate"))]
        (case (get-in resp [:headers :Content-Encoding])
          "gzip"
          (update-in resp [:body]
                     (fn [in]
                       (GZIPInputStream. in)))
          "deflate"
          (update-in resp [:body]
                     (fn [in]
                       (InflaterInputStream. in)))
          resp)))))

(defn wrap-proxy [client]
  (fn [{:keys [url] :as req}]
    (if (reseted-url? url)
      (client (assoc req :proxy? true))
      (try
        (client req)
        (catch SocketException e
          (if (and (reset-e? e) (not (:proxy? req)))
            (do
              (add-reseted-url url)
              (client (assoc req :proxy? true)))
            (throw e)))))))

(defn wrap-exception [client]
  (fn [req]
    (try (client req)
         (catch ConnectException e
           {:status 450
            :headers {}})
         (catch UnknownHostException e
           {:status 451
            :headers {}})
         (catch Exception e
           (throw e)))))

(defn- assoc-if [map & kvs]
  "like assoc, but drop false value"
  (let [kvs (apply concat
                   (filter #(second %) (partition 2 kvs)))]
    (if (seq kvs) (apply assoc map kvs) map)))

(def request* (-> request
                  wrap-compression
                  wrap-proxy
                  wrap-redirects
                  wrap-exception))

(defn get
  [url & {:keys [last-modified user-agent]
          :or {user-agent "Mozilla/5.0 (X11; Linux x86_64)"}}]
  (request* (assoc-if {:url url}
                      :User-Agent user-agent
                      :If-Modified-Since last-modified)))
