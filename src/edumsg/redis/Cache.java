package edumsg.redis;

import redis.clients.jedis.Jedis;
import java.util.HashMap;
import java.util.Map;

public class Cache {

    public static Jedis redisCache = new Jedis("localhost", 6379);
    private static HashMap<String, String> details = new HashMap<String, String>();

    //READ OPERATIONS

    public static Map<String, String> returnUser(String username) {
        if (redisCache.exists(username)) {
            return redisCache.hgetAll(username);
        } else {
            return null;
        }
    }


    public static void cacheUser(String id, String username, String email, String name, String language, String country, String bio, String website, String created_at, String avatar_url, String overlay, String link_color, String background_color, String protected_tweets, String session_id) {
        details.put("id", id);
        details.put("username", username);
        details.put("email", email);
        details.put("name", name);
        details.put("language", language);
        details.put("country", country);
        details.put("bio", bio);
        details.put("website", website);
        details.put("created_at", created_at);
        details.put("avatar_url", avatar_url);
        details.put("overlay", overlay);
        details.put("link_color", link_color);
        details.put("background_color", background_color);
        details.put("protected_tweets", protected_tweets);
        details.put("session_id", session_id);

        redisCache.hmset("user:"+id, details);
    }

    public static void registerUser(String id, Map<String,String> registerDetails){
        redisCache.hmset(id,registerDetails);
    }

    public static void createTweet(String id, Map<String,String> tweetDetails){
        redisCache.hmset(id,details);
    }

    public static void  cacheList(String id){
        redisCache.
    }

    public static void main(String[] args) {
        redisCache.set("fdfd", "Fdf");
    }

}
