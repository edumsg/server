package edumsg.redis;

import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UserCache extends Cache {
    //new instance of shared pool to support multithreaded environments
    public static Jedis userCache = edumsg.redis.Cache.getRedisPoolResource();
    private static Pipeline userPipeline = userCache.pipelined();


    public static void userBgSave() {
        Runnable runnable = () -> {
            String res;
            res = userCache.bgsave();
            System.out.println(res);
        };
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(runnable, 0, 15, TimeUnit.MINUTES);
    }

    public static @Nullable Map<String, String> returnUser(String user_id) {
        if (userCache.exists("user:" + user_id)) {
            return userCache.hgetAll("user:" + user_id);
        } else {
            return null;
        }
    }


    public static void addUserToCacheList(String user_id) {
        if (user_id != null && !userCache.sismember("user:", user_id)) {
            userCache.sadd("users:", user_id);
        }
    }

    public static void removeUserFromCacheList(String user_id) {
        if (user_id != null) {
            userCache.srem("users:", user_id);
        }
    }

    public static String cacheUser(String user_id, Map<String, String> userDetails) {
        if (!Cache.checkNulls(userDetails)) {
            return userCache.hmset("user:" + user_id, userDetails);
        } else
            return "";
    }

    public static void cacheUserTweet(String user_id, String tweet_id) {
        if ((user_id != null) && (tweet_id != null)) {
            userCache.sadd("usertweets:" + user_id, tweet_id);
        }
    }

    public static void cacheFollowing(String user_id, String user_to_follow_id) {
        if ((user_id != null) && (user_to_follow_id != null)) {
            userCache.sadd("userfollowing:" + user_id, user_to_follow_id);
        }
    }

    public static void unFollow(String user_id, String user_being_followed_id) {
        userPipeline.srem("userfollowing:" + user_id, user_being_followed_id);
        userPipeline.srem("userfollowers:" + user_being_followed_id, user_id);
        userPipeline.sync();
    }

    public static void cacheFollowers(String user_id, String follower_id) {
        if ((user_id != null) && (follower_id != null)) {
            userCache.sadd("userfollowers:" + user_id, follower_id);
        }
    }

    public static void logoutUser(String user_id) {
        userCache.hdel("user:" + user_id, "session_id");
    }

    public static void cacheUserSession(String session_id, String user_id) {
        userCache.hset("sessions", session_id, user_id);
    }

    public static void mapUsernameID(String username, String id) {
        userCache.hset("usernameid", username, id); //maps username to id
    }

    public static String returnUserID(String username) {
        return userCache.hget("usernameid", username); //returns id based on username
    }


}
