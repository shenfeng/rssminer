(ns rssminer.import
  (:use [rssminer.handlers.subscriptions :only [add-subscription* subscribe]]
        [clojure.data.json :only [read-json]]
        [clojure.tools.logging :only [info error]]
        [ring.util.response :only [redirect]]
        (rssminer [util :only [session-get]]
                  [http :only [client parse-response]]
                  [config :only [rssminer-conf]]))
  (:import java.net.URI
           java.io.File
           rssminer.importer.Parser))

(def oauth2 {"redirect_uri" "http://localhost:9090/oauth2callback"
             "client_secret" "gQ-exryYQvjEW9OV_lqeh-uQ"
             "client_id" "1062014352023.apps.googleusercontent.com"
             "grant_type" "authorization_code"})

(def scope "https://www.google.com/reader/api/")
(def token-ep (URI. "https://accounts.google.com/o/oauth2/token"))
(def list-dp (URI. "https://www.google.com/reader/api/0/subscription/list"))

(defn subscribe-all [user-id items]
  (doseq [sub items]
    (let [{:keys [title url category]} (bean sub)]
      (subscribe url user-id title category))))

(defn opml-import [req]
  (let [^File file (-> req :params :file :tempfile)
        user-id (:id (session-get req :user))]
    (if (and file (> (.length file) 10))
      (subscribe-all user-id (Parser/parseOPML (slurp file)))
      {:status 400
       :body {:message "Please choose a file"}})))

(defn oauth2callback [req]
  (let [code (-> req :params :code)
        user-id (:id (session-get req :user))
        resp (-> client (.execPost token-ep {} (assoc oauth2 "code" code))
                 .get parse-response)]
    (if (= 200 (:status resp))
      (let [{:keys [access_token refresh_token]} (read-json (:body resp))
            data (-> client (.execGet list-dp {"Authorization"
                                               (str "OAuth " access_token)})
                     .get parse-response)]
        (if (= 200 (:status data))
          (let [items (Parser/parseGReaderSubs (:body data))]
            (info user-id "import greader" (count items))
            (subscribe-all user-id items)
            (redirect "/a"))
          (do (error "import greader" (:status data) "; code" code)
              (redirect "/"))))
      (do (error "get greader code" (:status resp))
          (redirect "/")))))

(defn greader-import [req]
  (let [host (if (= (@rssminer-conf :profile) :dev)
               "localhost:9090/" "rssminer.net/")]
    (redirect
     (str
      "https://accounts.google.com/o/oauth2/auth?redirect_uri=http://"
      host "oauth2callback&response_type=code"
      "&client_id=1062014352023.apps.googleusercontent.com"
      "&scope=https://www.google.com/reader/api/&access_type=offline"))))
