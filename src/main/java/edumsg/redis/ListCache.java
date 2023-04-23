package edumsg.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ListCache extends Cache {
    //new instance of shared pool to support multithreaded environments
    public static Jedis listCache = edumsg.redis.Cache.getRedisPoolResource();
    private static Pipeline listPipeline = listCache.pipelined();

    public static void listBgSave() {
        Runnable runnable = () -> {
            String res;
            res = listCache.bgsave();
            System.out.println(res);
        };
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(runnable, 0, 15, TimeUnit.MINUTES);
    }

    public static boolean listExists(String id) {
        return listCache.exists("lists:" + id);
    }

    public static void createList(String id, Map<String, String> members) {
        if (!Cache.checkNulls(members)) {
            listCache.hmset("list:" + id, members);
        }
    }

    public static void addMemberList(String list_id, String member_id) {
        if ((list_id != null) && (member_id != null)) {
            listCache.sadd("listmember:" + list_id, member_id);
        }
    }

    public static void deleteList(String list_id) {

    }

}
