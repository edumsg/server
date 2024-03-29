package edumsg.redis;

import edumsg.core.config;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Cache {
    protected static JedisPool redisPool;

    static {
        redisPool = getConnection();
    }

    public Jedis jedisCache = edumsg.redis.Cache.getRedisPoolResource();
    private Pipeline dmPipeline = jedisCache.pipelined();

    public static JedisPool getConnection() {
        URI redisURI;
        JedisPool jedis = null;
        try {
            redisURI = new URI(System.getenv("REDIS_URL"));
            System.err.println("Redis URI :" + redisURI);
            jedis = new JedisPool(redisURI);
        } catch (NullPointerException | URISyntaxException e) {
            String redisHost = config.getMain_host();
            System.err.println("Redis URI : " + redisHost + ":6379");
            jedis = new JedisPool(new JedisPoolConfig(), redisHost, 6379);
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

    public void BgSave() {
        Runnable runnable = () -> {
            String res;
            res = jedisCache.bgsave();
            System.out.println(res);
        };
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(runnable, 0, 15, TimeUnit.MINUTES);
    }


}
