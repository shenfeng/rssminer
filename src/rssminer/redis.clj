(ns rssminer.redis
  (:use ring.middleware.session.store)
  (:import java.util.UUID
           rssminer.RedisBackendSessionStore
           [redis.clients.jedis JedisPool Jedis JedisPoolConfig]))

(defonce redis-client (atom nil))

(defn set-redis-client! [^String host] ;; called when app start
  (when (nil? @redis-client)
    (reset! redis-client (JedisPool. (JedisPoolConfig.) host))))

(defn- gen-key [data]
  (if-let [id (-> data :user :id)]
    (str "z" (Integer/toString (- Integer/MAX_VALUE id) 35) "k")
    (str (UUID/randomUUID))))

(deftype RedisStore [^RedisBackendSessionStore db]
  SessionStore
  (read-session [_ key]
    (if (nil? key)
      {}
      (if-let [bs (.get db ^String key)]
        (read-string bs)
        {})))
  (write-session [_ key data]
    (let [^String key (or key (gen-key data))]
      (.put db key (pr-str data))
      key))
  (delete-session [_ key]
    (when-not (nil? key)
      (.delete db key))))

(defn redis-store [expire]
  (when (nil? @redis-client)
    (set-redis-client! "127.0.0.1"))
  (RedisStore. (RedisBackendSessionStore.
                @redis-client expire)))

(def ^String fetcher-key "fetcher-queue")

(def ^"[Ljava.lang.String;" fetcher-key-arr (into-array (list fetcher-key)))

(defn fetcher-enqueue [data]
  (let [^JedisPool client @redis-client
        ^Jedis j (.getResource client)]
    (try
      (.rpush j fetcher-key (pr-str data)) ; tail
      (finally (.returnResource client j)))))

(defn fetcher-dequeue [timeout]         ; seconds
  (let [^JedisPool client @redis-client
        ^Jedis j (.getResource client)]
    (try
      (when-let [d (.blpop j (int timeout) fetcher-key-arr)]
        (-> d second read-string))
      (finally (.returnResource client j)))))
