(ns rssminer.redis
  (:use ring.middleware.session.store)
  (:import java.util.UUID
           [redis.clients.jedis JedisPool Jedis JedisPoolConfig]))

(defonce redis-client (atom nil))

(defn set-redis-client [^String host] ;; called when app start
  (when (nil? @redis-client)
    (reset! redis-client (JedisPool. (JedisPoolConfig.) host))))

(defn- gen-key [data]
  (if-let [id (-> data :user :id)]
    (str "z" (Integer/toString (- Integer/MAX_VALUE id) 35) "k")
    (str (UUID/randomUUID))))

(deftype RedisStore [^JedisPool db ^int expire]
  SessionStore
  (read-session [_ key]
    (if (nil? key)
      {}
      (let [^Jedis j (.getResource db)]
        (try
          (if-let [bs (.get j ^String key)]
            (read-string bs)
            {})
          (finally
           (.returnResource db j))))))
  (write-session [_ key data]
    (let [^String key (or key (gen-key data))
          ^Jedis j (.getResource db)]
      (try
        (.setex j key expire (pr-str data))
        (finally
         (.returnResource db j)))
      key))
  (delete-session [_ key]
    (when-not (nil? key)
      (let [^Jedis j (.getResource db)]
        (try
          (.del j ^"[Ljava.lang.String;" (into-array '(key)))
          (finally
           (.returnResource db j)))))))

(defn redis-store [expire]
  (when (nil? @redis-client)
    (set-redis-client "127.0.0.1"))
  (RedisStore. @redis-client expire))

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
