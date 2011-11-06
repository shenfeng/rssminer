(ns rssminer.redis
  (:use ring.middleware.session.store)
  (:import java.util.UUID
           [redis.clients.jedis JedisPool Jedis JedisPoolConfig]
           [java.io ObjectOutputStream ByteArrayOutputStream
            ByteArrayInputStream ObjectInputStream]))

(defn- serialize [obj]
  (let [bao (ByteArrayOutputStream.)
        os (ObjectOutputStream. bao)]
    (.writeObject os obj)
    (.toByteArray bao)))

(defn- deserialize [ba]
  (let [oi (ObjectInputStream. (ByteArrayInputStream. ba))]
    (.readObject oi)))

(deftype RedisStore [^JedisPool db ^int expire]
  SessionStore
  (read-session [_ key]
    (if (nil? key)
      {}
      (let [^Jedis j (.getResource db)]
        (try
          (deserialize (.get j (.getBytes ^String key)))
          (finally
           (.returnResource db j))))))
  (write-session [_ key data]
    (let [^String key (or key (str (UUID/randomUUID)))
          ^Jedis j (.getResource db)]
      (try
        (.setex j (.getBytes key) expire (bytes (serialize data)))
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


(defn redis-store [expire ^String host]
  (RedisStore. (JedisPool. (JedisPoolConfig.) host) expire))
