(ns rssminer.handlers.proxy
  (:use (rssminer [http :only [client]]
                  [util :only [assoc-if]]
                  [config :only [rssminer-conf socks-proxy no-proxy]]))
  (:import rssminer.ResponseFutureProxy
           ring.adapter.netty.HttpResponseFuture
           java.net.URI))

(defn- compute-send-header [req]
  (let [headers (:headers req)]
    (assoc-if {"X-Forwarded-For" (:remote-addr req)}
              "User-Agent" (headers "user-agent")
              "If-Modified-Since" (headers "if-modified-since")
              "Cache-Control" (headers "cache-control"))))

(defn handle-proxy [req]
  (let [uri (-> req :params :u)
        headers (compute-send-header req)]
    {:status 200
     :body (ResponseFutureProxy.    ; understand by async-ring-handler
            (.execGet client (URI. uri) headers
                      (if (:proxy @rssminer-conf)
                        socks-proxy no-proxy)))}))
