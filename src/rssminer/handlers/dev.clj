(ns  rssminer.handlers.dev
  (:use [me.shenfeng.http.server :only [defwshandler send-mesg on-close]]
        [clojure.tools.logging :only [info]]
        rssminer.config
        [compojure.core :only [defroutes GET routes]]))

(def clients (atom {}))

(defwshandler ws-handler [req] con
  (swap! clients assoc con 1)
  (on-close con (fn [status]
                  (swap! clients dissoc con)
                  (info con "closed, status" status))))

(defn resouce-changed [req]
  (doseq [client (keys @clients)]
    (send-mesg client "reload"))
  {:status 200 :body "ok"})

(def dev-routes
  (let [handler (routes
                 (GET "/ws" [] ws-handler)
                 (GET "/c" [] resouce-changed))]
    (fn [req]
      (when (= (cfg :profile) :dev)
        (handler req)))))
