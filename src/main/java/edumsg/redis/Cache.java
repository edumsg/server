package edumsg.redis;

import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Cache {
    protected static JedisPool redisPool = getConnection();

    private static JedisPool getConnection() {
        URI redisURI = null;
        System.err.println("Redis URI 1");
        try {
            redisURI = new URI(System.getenv("REDIS_URL"));
            System.err.println("Redis URI 2:" + redisURI);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        JedisPool jedis = null;
        if ( redisURI == null ) {
            System.err.println("Redis URI 3");
            jedis = new JedisPool(new JedisPoolConfig(),"localhost", 6379);
        }
        else {
            jedis = new JedisPool(redisURI);
        }
        System.err.println(redisURI);
        return jedis;
    }

    protected static boolean checkNulls(Map<String, String> map) {
        return map.containsValue(null);
    }

    //converts HashMaps to ConcurrentHashmMaps
    protected static ConcurrentMap<String,String> toConcurrentMap(Map<String, String> map) {
        String[] mapStrings = map.toString().split(",");
        CopyOnWriteArrayList<String> mapStringsConcurrent = new CopyOnWriteArrayList<>(Arrays.asList(mapStrings));
        ConcurrentMap<String,String> result = mapStringsConcurrent.parallelStream().map(hash -> braceRemover(hash).trim().split("=")).collect(Collectors.toConcurrentMap(key -> key[0], value -> value[1]));

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
