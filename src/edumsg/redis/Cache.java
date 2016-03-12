package edumsg.redis;

import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import java.util.Map;

public class Cache {
    public static Jedis redisCache = new Jedis("localhost", 6379);
    private static Pipeline pipe = redisCache.pipelined();

    /////////////////////////////////////
    ////////////USER OPERATIONS//////////
    /////////////////////////////////////

    @Nullable
    public static Map<String, String> returnUser(String user_id) {
        if (redisCache.exists("user:"+user_id)) {
            return redisCache.hgetAll("user:"+user_id);
        } else {
            return null;
        }
    }

    public static void cacheUser(String user_id, Map<String, String> userDetails) {
        if (!Cache.checkNulls(userDetails)) {
            redisCache.hmset("user:" + user_id, userDetails);
        }
    }

    public static void cacheUserTweet(String user_id, String tweet_id) {
        if ((user_id != null) && (tweet_id != null)) {
            redisCache.sadd("usertweets:" + user_id, tweet_id);
        }
    }

    public static void cacheFollowing(String user_id, String user_to_follow_id) {
        if ((user_id != null) && (user_to_follow_id != null)) {
            redisCache.sadd("userfollowing:" + user_id, user_to_follow_id);
        }
    }

    public static void unFollow(String user_id, String user_being_followed_id){
        pipe.srem("userfollowing:"+user_id, user_being_followed_id);
        pipe.srem("userfollowers:"+user_being_followed_id,user_id);
        pipe.sync();
    }

    public static void cacheFollowers(String user_id, String follower_id) {
        if ((user_id != null) && (follower_id != null)) {
            redisCache.sadd("userfollowers:" + user_id, follower_id);
        }
    }

    public static void logoutUser(String user_id){
        redisCache.hdel("user:"+user_id,"session_id");
    }

    public static void mapUsernameID(String username, String id){
        redisCache.hset("usernameid", username, id); //maps username to id
    }

    public static String returnUserID(String username){
        return redisCache.hget("usernameid",username); //returns id based on username
    }

    /////////////////////////////////////
    ///////////LIST OPERATIONS///////////
    /////////////////////////////////////

    public static boolean listExists(String id) {
        return redisCache.exists("lists:" + id);
    }

    public static void createList(String id, Map<String, String> members) {
        if (!Cache.checkNulls(members)) {
            redisCache.hmset("list:" + id, members);
        }
    }

    public static void addMemberList(String list_id, String member_id) {
        if ((list_id != null) && (member_id != null)) {
            redisCache.sadd("listmember:" + list_id, member_id);
        }
    }

    public static void deleteList(String list_id){

    }

    /////////////////////////////////////
    ///////////TWEET OPERATIONS//////////
    ////////////////////////////////////

    public static void createTweet(String id, Map<String, String> tweetDetails) {
        if (!Cache.checkNulls(tweetDetails)) {
            redisCache.hmset("tweet:" + id, tweetDetails);
        }
    }

    @Nullable
    public static Map<String, String> returnTweet(String id) {
        if (redisCache.exists("tweet:" + id)) {
            return redisCache.hgetAll("tweet:" + id);
        } else {
            return null;
        }

    }

    public static void deleteTweet(String tweet_id) {
        if (tweet_id != null) {
            String user =  redisCache.hget("tweet:"+tweet_id,"creator_id");
            pipe.del("tweet:"+tweet_id);
            pipe.srem("usertweets:"+user,tweet_id);
            pipe.sync();
        }
    }
    private static boolean checkNulls(Map<String, String> map) {
        return map.containsValue(null);
    }


}
