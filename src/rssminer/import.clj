(ns rssminer.import
  (:use [rssminer.handlers.subscriptions :only [add-subscription*]]
        [clojure.data.json :only [read-json]]
        (rssminer [util :only [session-get]]
                  [http :only [client parse-response]]))
  (:import java.net.URI
           rssminer.importer.Parser))

(def oauth2 {"redirect_uri" "http://localhost:9090/oauth2callback"
             "client_secret" "gQ-exryYQvjEW9OV_lqeh-uQ"
             "client_id" "1062014352023.apps.googleusercontent.com"
             "grant_type" "authorization_code"})

(def scope "https://www.google.com/reader/api/")
(def token-ep (URI. "https://accounts.google.com/o/oauth2/token"))
(def list-dp (URI. "https://www.google.com/reader/api/0/subscription/list"))

(defn opml-import [req]
  (let [^java.io.File file (-> req :params :file :tempfile)]
    (if (and file (> (.length file) 10))
      (let [user-id (:id (session-get req :user))
            subs (map bean (Parser/parseOPML (slurp file)))]
        (map #(add-subscription* (:url %) user-id
                                 :category (:category %)
                                 :title (:title %)) subs))
      {:status 400
       :message "Please choose a file"})))

(defn oauth2callback [req]
  (let [code (-> req :params :code)
        resp (-> client (.execPost token-ep {} (assoc oauth2 "code" code))
                 .get parse-response)]
    (if (= 200 (:status resp))
      (let [{:keys [access_token refresh_token]} (read-json (:body resp))
            data (-> client (.execGet list-dp {"Authorization"
                                               (str "OAuth " access_token)})
                     .get parse-response)]
        (if (= 200 (:status data))
          (map bean (Parser/parseGReaderSubs (:body data))))))))
