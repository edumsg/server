package edumsg.redis;

import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Pipeline;

import java.util.Map;

public class UserCache extends Cache {
    private Pipeline userPipeline = jedisCache.pipelined();

    public void logoutUser(String user_id) {
        jedisCache.hdel("user:" + user_id, "session_id");
    }

    public void cacheUserTweet(String user_id, String tweet_id) {
        if ((user_id != null) && (tweet_id != null)) {
            jedisCache.sadd("usertweets:" + user_id, tweet_id);
        }
    }

    public void cacheFollowing(String user_id, String user_to_follow_id) {
        if ((user_id != null) && (user_to_follow_id != null)) {
            jedisCache.sadd("userfollowing:" + user_id, user_to_follow_id);
        }
    }

    public void unFollow(String user_id, String user_being_followed_id) {
        userPipeline.srem("userfollowing:" + user_id, user_being_followed_id);
        userPipeline.srem("userfollowers:" + user_being_followed_id, user_id);
        userPipeline.sync();
    }

    public void cacheFollowers(String user_id, String follower_id) {
        if ((user_id != null) && (follower_id != null)) {
            jedisCache.sadd("userfollowers:" + user_id, follower_id);
        }
    }

    public void cacheUserSession(String session_id, String user_id) {
        jedisCache.hset("sessions", session_id, user_id);
    }

    public void mapUsernameID(String username, String id) {
        jedisCache.hset("usernameid", username, id); //maps username to id
    }

    public String returnUserID(String username) {
        return jedisCache.hget("usernameid", username); //returns id based on username
    }

    public @Nullable Map<String, String> returnUser(String user_id) {
        if (jedisCache.exists("user:" + user_id)) {
            return jedisCache.hgetAll("user:" + user_id);
        } else {
            return null;
        }
    }

    public void addUserToCacheList(String user_id) {
        if (user_id != null && !jedisCache.sismember("user:", user_id)) {
            jedisCache.sadd("users:", user_id);
        }
    }

    public void removeUserFromCacheList(String user_id) {
        if (user_id != null) {
            jedisCache.srem("users:", user_id);
        }
    }

    public String cacheUser(String user_id, Map<String, String> userDetails) {
        if (!Cache.checkNulls(userDetails)) {
            return jedisCache.hmset("user:" + user_id, userDetails);
        } else
            return "";
    }


}
