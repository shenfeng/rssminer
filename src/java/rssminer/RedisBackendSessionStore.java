package rssminer;

import java.util.concurrent.ConcurrentHashMap;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Only use when there is only one web server, or consistent routing
 * 
 * Sine for a very long time, there should be only one server, this should be
 * fine
 * 
 * @author feng
 * 
 */
public class RedisBackendSessionStore {

	private final JedisPool redis;
	private final int expire;
	private ConcurrentHashMap<String, String> map;

	public RedisBackendSessionStore(JedisPool db, int expire) {
		this.redis = db;
		this.expire = expire;
		map = new ConcurrentHashMap<String, String>(100, 0.75f, 4);
	}

	public RedisBackendSessionStore(JedisPool db, int expire, int concurrent) {
		this.redis = db;
		this.expire = expire;
		map = new ConcurrentHashMap<String, String>(100, 0.75f, concurrent);
	}

	public String get(String key) {
		String result = map.get(key);
		if (result == null) {
			Jedis j = redis.getResource();
			try {
				// app newly start, local cache is missing
				result = j.get(key);
				if (result != null) {
					map.put(key, result);
				}
			} finally {
				redis.returnResource(j);
			}
		}
		return result;
	}

	public void put(String key, String data) {
		// TODO maybe a job to check expired keys.
		// local cache are not expired, anyway, maybe fine. app is restarted
		// frequently
		map.put(key, data);
		Jedis j = redis.getResource();
		try {
			j.setex(key, expire, data);
		} finally {
			redis.returnResource(j);
		}
	}

	public void delete(String key) {
		map.remove(key);
		Jedis j = redis.getResource();
		try {
			j.del(key);
		} finally {
			redis.returnResource(j);
		}
	}
}
