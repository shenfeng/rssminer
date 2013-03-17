(ns rssminer.import
  (:use [rssminer.handlers.subscriptions :only [subscribe]]
        [clojure.data.json :only [read-json]]
        [clojure.tools.logging :only [info error]]
        [ring.util.response :only [redirect]]
        [rssminer.config :only [cfg]]
        (rssminer [util :only [user-id-from-session ignore-error]]
                  [http :only [request]]))
  (:import rssminer.Utils))

(def oauth2 {"redirect_uri" "http://rssminer.net/oauth2callback"
             "client_secret" "gQ-exryYQvjEW9OV_lqeh-uQ"
             "client_id" "1062014352023.apps.googleusercontent.com"
             "grant_type" "authorization_code"})

(def token-ep "https://accounts.google.com/o/oauth2/token")
(def list-dp "https://www.google.com/reader/api/0/subscription/list")

(defn subscribe-all [uid items]
  (doseq [sub items]
    (let [{:keys [title url category]} (bean sub)]
      (subscribe url uid title category))))

(defn- finish-import [data]
  (if (= 200 (:status data))
    (do
      (info "import from greader success")
      (redirect "/a?gw=1"))              ; google import wait
    (let [msg (or (:body data) data)]
      (error "import from greader failed" msg)
      (redirect (str "/a?ge=" msg)))))

(defn oauth2callback [req]
  (if-let [code (-> req :params :code)]
    (let [uid (user-id-from-session req)
          resp (request {:url token-ep :post (assoc oauth2 "code" code)})]
      (when (and uid (not= uid (:id (cfg :demo-user))))
        (if (= 200 (:status resp))
          (let [{:keys [access_token refresh_token]} (read-json (:body resp))
                data (request {:url list-dp
                               :headers {"Authorization"
                                         (str "OAuth " access_token)}})]
            (if (= 200 (:status data))   ; do import
              (let [items (Utils/parseGReaderSubs (:body data))]
                (info uid "import greader" (count items))
                (subscribe-all uid items)))
            (finish-import data))
          (finish-import resp))))        ; error
    (finish-import "import failed")))

(defn greader-import [req]
  (redirect
   (str "https://accounts.google.com/o/oauth2/auth"
        "?redirect_uri=http://rssminer.net/oauth2callback&response_type=code"
        "&client_id=1062014352023.apps.googleusercontent.com"
        "&scope=https://www.google.com/reader/api/&access_type=offline")))
