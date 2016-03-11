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

    public static void cacheUserTweet(String user_id, String tweet_id){
        redisCache.sadd("usertweets:"+user_id, tweet_id);
    }

    public static void cacheFollowing(String user_id, String user_to_follow_id){
        redisCache.sadd("userfollowing:"+user_id,user_to_follow_id);
    }

    public static void cacheFollowers(String user_id, String follower_id){
        redisCache.sadd("userfollowers:"+user_id,follower_id);
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

    public static void addMemberList(String list_id, String member_id) {
        redisCache.sadd("listmember:" + list_id, member_id);
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
