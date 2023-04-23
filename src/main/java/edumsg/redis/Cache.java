package edumsg.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class Cache {
    protected static JedisPool redisPool;

    static {
        try {
            redisPool = getConnection();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static JedisPool getConnection() throws UnknownHostException {
        URI redisURI;
        InetAddress localHost = InetAddress.getLocalHost();
        JedisPool jedis = null;
        try {
            redisURI = new URI(System.getenv("REDIS_URL"));
            System.err.println("Redis URI :" + redisURI);
            jedis = new JedisPool(redisURI);
        } catch (NullPointerException | URISyntaxException e) {
            System.err.println("Redis URI : local-6379");
            jedis = new JedisPool(new JedisPoolConfig(), localHost.getHostAddress(), 6379);
        }
        return jedis;
    }

    public static Jedis getRedisPoolResource() {
        Jedis jedis = null;
        try {
            jedis = redisPool.getResource();
        } catch (Exception e) {
            System.err.println("Cannot get RedisPool resource");
            System.err.println(e.getMessage());
        }

        if (jedis == null) {
            System.err.println("No User Cache Created !!");
        }

        return jedis;
    }


    protected static boolean checkNulls(Map<String, String> map) {
        return map.containsValue(null);
    }

    //converts HashMaps to ConcurrentHashmMaps
    protected static ConcurrentMap<String, String> toConcurrentMap(Map<String, String> map) {
        String[] mapStrings = map.toString().split(",");
        CopyOnWriteArrayList<String> mapStringsConcurrent = new CopyOnWriteArrayList<>(Arrays.asList(mapStrings));
        ConcurrentMap<String, String> result = mapStringsConcurrent.parallelStream().map(hash -> braceRemover(hash).trim().split("=")).collect(Collectors.toConcurrentMap(key -> key[0], value -> value[1]));

        return result;
    }


    protected static String braceRemover(String x) {
        if (x.startsWith("{") && x.endsWith("}")) {
            return x.substring(1, x.length() - 1);
        } else {
            if (x.startsWith("{")) {
                return x.substring(1);
            } else {
                if (x.endsWith("}")) {
                    return x.substring(0, x.length() - 1);
                } else {
                    return x;
                }
            }
        }
    }


}
