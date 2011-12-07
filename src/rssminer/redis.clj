(ns rssminer.redis
  (:use ring.middleware.session.store)
  (:import java.util.UUID
           [redis.clients.jedis JedisPool Jedis JedisPoolConfig]))

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
    (let [^String key (or key (str (UUID/randomUUID)))
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


(defn redis-store [expire ^String host]
  (RedisStore. (JedisPool. (JedisPoolConfig.) host) expire))
