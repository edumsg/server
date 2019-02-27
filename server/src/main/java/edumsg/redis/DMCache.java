package edumsg.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static edumsg.redis.Cache.redisPool;

/**
 * Created by ahmed on 5/8/16.
 */
public class DMCache extends Cache {
    //new instance of shared pool to support multithreaded environments
    public static Jedis dmCache = redisPool.getResource();
    private static Pipeline dmPipeline = dmCache.pipelined();

    public static void dmBgSave(){
        Runnable runnable = () -> {
            String res;
            res = dmCache.bgsave();
            System.out.println(res);
        };
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(runnable, 0, 15, TimeUnit.MINUTES);
    }

}
