(ns rssminer.http
  (:require [clojure.string :as str])
  (:import [java.net URI URL URLEncoder HttpURLConnection]))

(defn- ^String build-body [params]
  (apply str
         (interpose
          "&"
          (map (fn [[k v]] (str (URLEncoder/encode (str k) "utf8")
                               "="
                               (URLEncoder/encode (str v) "utf8"))) params))))

(defn request [{:keys [url headers post]}]
  (try
    (let [^HttpURLConnection con (doto (.. (URL. url) openConnection)
                                   (.setReadTimeout 6500)
                                   (.setConnectTimeout 6500))]
      (.setInstanceFollowRedirects con false)
      (doseq [header headers]
        (.setRequestProperty con (name (key header)) (val header)))
      (when post
        (.setDoOutput con true)
        (.setRequestMethod con "POST")
        (.. con getOutputStream (write (.getBytes (build-body post)))))
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
         :body (if (= status 200) (slurp (.getInputStream con))
                   (.disconnect con))}))
    (catch Exception e
      {:status 410
       :body (.getMessage e)})))
