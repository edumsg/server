package edumsg.redis;

import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import java.util.Map;

public class Cache {
    public static Jedis redisCache = new Jedis("localhost", 6379);

    /////////////////////////////////////
    //USER OPERATIONS
    /////////////////////////////////////

    @Nullable
    public static Map<String, String> returnUser(String username) {
        if (redisCache.exists(username)) {
            return redisCache.hgetAll(username);
        } else {
            return null;
        }
    }

    public static void cacheUser(String id, Map<String, String> userDetails) {
        redisCache.hmset("user:" + id, userDetails);
    }

    public static void registerUser(String id, Map<String, String> registerDetails) {
        redisCache.hmset(id, registerDetails);
    }

    /////////////////////////////////////
    ///////////LIST OPERATIONS///////////
    /////////////////////////////////////

    public static boolean listExists(String id) {
        return redisCache.exists("lists:" + id);
    }


    public static void createList(String id, Map<String, String> members) {
        redisCache.hmset("list:" + id, members);
    }

    public static void addMemberList(String id, String member_id) {
        redisCache.sadd("listmember:" + id, member_id);
    }




    public static void createTweet(String id, Map<String, String> tweetDetails) {
        redisCache.hmset("tweet:" + id, tweetDetails);
    }

    @Nullable
    public static Map<String, String> returnTweet(String id) {
        if (redisCache.exists("tweet:" + id)) {
            return redisCache.hgetAll("tweet:" + id);
        } else {
            return null;
        }

    }


}
