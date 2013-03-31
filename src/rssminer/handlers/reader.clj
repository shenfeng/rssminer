(ns rssminer.handlers.reader
  (:use (rssminer [util :only [md5-sum json-str2 defhandler resolve-url]]
                  [search :only [search*]])
        [clojure.java.io :only [resource]]
        [org.httpkit.server :only [with-channel send!]]
        [ring.util.response :only [redirect]]
        [rssminer.database :only [mysql-query mysql-insert]])
  (:require [rssminer.config :as cfg]
            [clojure.string :as str]
            [org.httpkit.client :as http]
            [rssminer.db.subscription :as sdb]
            [rssminer.db.user :as udb]
            [rssminer.db.feed :as db]
            [rssminer.tmpls :as tmpls])
  (:import org.httpkit.BytesInputStream
           rssminer.jsoup.HtmlUtils
           java.net.URI
           java.io.ByteArrayInputStream))

(defn show-unsupported-page [req]
  (tmpls/browser))

(defhandler landing-page [req r mobile? return-url]
  (if (= r "d")       ; redirect to /demo
    (redirect "/demo")
    (if (cfg/real-user? req)
      (redirect (if mobile? "/m" "/a"))
      (let [body (if mobile? (tmpls/m-landing)
                     (tmpls/landing {:return-url return-url}))]
        (if (cfg/demo-user? req) {:status 200
                                  :session nil ;; delete cookie
                                  :session-cookie-attrs {:max-age -1}
                                  :body body}
            body)))))

(defn- get-subs [uid]
  (filter (fn [s]
            (and (:title s) (> (:total s) 0)))
          (sdb/fetch-subs uid)))

(defhandler show-app-page [req uid gw ge mobile?]
  (if (cfg/demo-user? req)
    (assoc (redirect "/") :session nil ;; delete cookie
           :session-cookie-attrs {:max-age -1})
    (if mobile?
      (redirect "/m")
      (when-let [user (udb/find-by-id uid)]
        (tmpls/app {:email (:email user)
                    :md5 (-> user :email md5-sum)
                    :data (json-str2 {:user user
                                      :subs (get-subs uid)
                                      :gw gw      ; google import wait
                                      :ge ge      ; google import error
                                      :static_server (cfg/cfg :static-server)})})))))

(defhandler show-demo-page [req mobile?]
  (if (cfg/real-user? req)
    (assoc (redirect "/?r=d") :session nil ;; delete cookie
           :session-cookie-attrs {:max-age -1})
    (let [user (udb/find-by-email "demo@rssminer.net")]
      ;; reload settings
      (swap! cfg/rssminer-conf assoc :demo-user user)
      (if mobile?
        (assoc (redirect "/m") :session user)
        {:body (tmpls/app {:email (:email user)
                           :md5 (-> user :email md5-sum)
                           :demo true
                           :data (json-str2 {:user user
                                             :subs (get-subs (:id user))
                                             :demo true
                                             :static_server (cfg/cfg :static-server)})})
         :headers {"Content-Type" "text/html; charset=utf8"}
         :status 200
         :session user}))))

(defhandler search [req q limit tags authors fs offset uid]
  (search* q tags authors uid limit offset (= fs "1")))

;;; favicon is a BytesInputStream
(defn- save-and-response [code hostname favicon channel]
  (mysql-insert :favicon {:code code :hostname hostname
                          :favicon (when (= BytesInputStream (class favicon))
                                     (.bytes favicon))})
  (if (= 200 code)
    (send! channel {:status 200
                    :headers {"Content-Type" "image/x-icon"}
                    :body favicon})
    (send! channel {:status 404})))

(defn- callback [{:keys [status error body opts headers]}]
  (let [{:keys [try-times hostname response-channel]} opts]
    (cond error
          (save-and-response 404 hostname nil response-channel)

          (or (= 301 status) (= 302 status))
          (if-let [url (resolve-url (:url opts) (:location headers))]
            (if (< try-times 5)
              (http/request (assoc opts :url url :try-times (inc try-times))
                            callback)
              ;; 5 requests, that's too many
              (save-and-response 404 hostname nil response-channel))
            ;; no url for redirection
            (save-and-response 404 hostname nil response-channel))

          (= :html (:expected-result opts))
          (let [base (URI/create (:url opts))
                url (HtmlUtils/extractFavicon body (URI/create base))
                url (if url (.toString url)
                        (str "http://" (.getHost base) "/favicon.ico"))]
            (http/request (assoc opts :url url
                                 :expected-result :binary
                                 :try-times (inc try-times))))

          (= :binary (:expected-result opts))
          (save-and-response 200 hostname body response-channel))))

(defn- fetch-favicon [req hostname channel]
  (let [url (str "http://" hostname "/")]
    (http/request {:url url
                   :method :get
                   :headers {"user-agent" (get req [:headers "user-agent"])}
                   :hostname hostname
                   :response-channel channel
                   :expected-result :html
                   :try-times 1} callback)))

(defhandler get-favicon [req h]
  (if (get-in req [:headers "if-modified-since"])
    {:status 304}
    (if-let [hostname (str/reverse h)]
      (let [cache (first (mysql-query ["SELECT favicon, code FROM favicon
                           WHERE hostname = ?" hostname]))]
        (if cache
          (if (= 200 (:code cache))
            {:status 200
             :header {"Content-Type" "image/x-icon"}
             :body (ByteArrayInputStream. (:favicon cache))}
            {:status 200})
          (with-channel req channel ;; cache miss
            (fetch-favicon req hostname channel))))
      {:status 404})))

(defhandler save-feedback [req uid email feedback refer]
  (udb/save-feedback {:email email
                      :ip (:remote-addr req)
                      :feedback feedback
                      :refer refer
                      :user_id uid})
  "ok")
