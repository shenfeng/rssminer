(ns freader.http
  (:import [java.net URI URL Proxy Proxy$Type InetSocketAddress
            HttpURLConnection SocketException]
           [java.util.zip InflaterInputStream GZIPInputStream]))

(def ^{:private true}
  socks-proxy (Proxy. Proxy$Type/SOCKS
                      (InetSocketAddress. "localhost" 3128)))

(def ^{:private true}  no-proxy Proxy/NO_PROXY)

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
                             (recur (inc i) (assoc headers (keyword k) v))
                             headers)))]
      {:status (.getResponseCode con)
       :headers resp-headers
       :body (.getInputStream con)})))

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

(defn extract-host [host]
  (let [uri (URI. host)
        port (if (= -1 (.getPort uri)) ""
                 (str ":" (.getPort uri)))
        schema (.getScheme uri)
        host (.getHost uri)]
    (str schema "://" host port)))

(let [blacks (atom #{})
      black? (fn [url]
               (@blacks (extract-host url)))
      add (fn [url]
            (swap! blacks conj (extract-host url)))
      reset? (fn [e]
               (= "Connection reset" (.getMessage e)))]
  (defn wrap-proxy [client]
    (fn [{:keys [url] :as req}]
      (if (black? url)
        (client (assoc req :proxy? true))
        (try
          (client req)
          (catch SocketException e
            (when (reset? e)
              (add url)
              (client (assoc req :proxy? true)))))))))

(def request* (-> request
                  wrap-compression
                  wrap-proxy
                  wrap-redirects))

(defn http-get [req]
  (request* req))
