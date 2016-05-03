package edumsg.redis;

import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Cache {
    public static JedisPool redisPool = new JedisPool(new JedisPoolConfig(), "localhost", 6379);
    public static Jedis userCache = redisPool.getResource();
    public static Jedis tweetCache = redisPool.getResource();
    public static Jedis dmCache = redisPool.getResource();
    public static Jedis listCache = redisPool.getResource();
    private static Pipeline userPipeline = userCache.pipelined();
    private static Pipeline tweetPipeline = userCache.pipelined();
    private static Pipeline dmPipeline = userCache.pipelined();
    private static Pipeline listPipeline = userCache.pipelined();

    public static void userBgSave(){
        Runnable runnable = () -> {
            String res;
            res = userCache.bgsave();
            System.out.println(res);
        };
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(runnable, 0, 15, TimeUnit.MINUTES);
    }

    public static void tweetBgSave(){
        Runnable runnable = () -> {
            String res;
            res = tweetCache.bgsave();
            System.out.println(res);
        };
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(runnable, 0, 15, TimeUnit.MINUTES);
    }

    public static void dmBgSave(){
        Runnable runnable = () -> {
            String res;
            res = dmCache.bgsave();
            System.out.println(res);
        };
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(runnable, 0, 15, TimeUnit.MINUTES);
    }

    public static void listBgSave(){
        Runnable runnable = () -> {
            String res;
            res = listCache.bgsave();
            System.out.println(res);
        };
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(runnable, 0, 15, TimeUnit.MINUTES);
    }

    /////////////////////////////////////
    ////////////USER OPERATIONS//////////
    /////////////////////////////////////

    @Nullable
    public static Map<String, String> returnUser(String user_id) {
        if (userCache.exists("user:" + user_id)) {
            return userCache.hgetAll("user:" + user_id);
        } else {
            return null;
        }
    }


    public static void addUserToCacheList(String user_id) {
        if (user_id != null && !userCache.sismember("user:",user_id)) {
            userCache.sadd("users:",user_id);
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
        }
        else
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

    public static void cacheUserSession(String user_id, String session_id) {
        userCache.hset("user:" + user_id, "session_id", session_id);
    }

    public static void mapUsernameID(String username, String id) {
        userCache.hset("usernameid", username, id); //maps username to id
    }

    public static String returnUserID(String username) {
        return userCache.hget("usernameid", username); //returns id based on username
    }

    /////////////////////////////////////
    ///////////LIST OPERATIONS///////////
    /////////////////////////////////////

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

    /////////////////////////////////////
    ///////////TWEET OPERATIONS//////////
    ////////////////////////////////////

    public static void cacheTweet(String id, Map<String, String> tweetDetails) {
        if (!Cache.checkNulls(tweetDetails)) {
            tweetCache.hmset("tweet:" + id, tweetDetails);
        }
    }

    @Nullable
    public static Map<String, String> returnTweet(String id) {
        if (tweetCache.exists("tweet:" + id)) {
            return tweetCache.hgetAll("tweet:" + id);
        } else {
            return null;
        }

    }

    public static void deleteTweet(String tweet_id) {
        if (tweet_id != null) {
            String user = tweetCache.hget("tweet:" + tweet_id, "creator_id");
            tweetPipeline.del("tweet:" + tweet_id);
            tweetPipeline.srem("usertweets:" + user, tweet_id);
            tweetPipeline.sync();
        }
    }

    public static CopyOnWriteArrayList<ConcurrentMap<ConcurrentMap<String, String>, ConcurrentMap<String, String>>> getTimeline(String user_id) {
        CopyOnWriteArrayList<ConcurrentMap<String, String>> tweets = new CopyOnWriteArrayList<>();  // Array list of tweets only
        tweetCache.smembers("userfollowing:" + user_id).parallelStream()
                .forEachOrdered(user -> getTweets(user).parallelStream()
                        .forEachOrdered(tweet_id -> tweets.add(toConcurrentMap(returnTweet(tweet_id)))));

        CopyOnWriteArrayList<ConcurrentMap<ConcurrentMap<String, String>, ConcurrentMap<String, String>>> users_and_tweets = new CopyOnWriteArrayList<>();  // Array list of user and tweets map

        ConcurrentMap<ConcurrentMap<String, String>, ConcurrentMap<String, String>> temp = new ConcurrentHashMap<>();

        tweets.parallelStream().forEachOrdered(tweet_map -> {
           users_and_tweets.add(new ConcurrentHashMap() {{put(tweet_map, toConcurrentMap(returnUser(tweet_map.get("creator_id"))));}});
            temp.clear();
        });
        return users_and_tweets;
    }

    public static Set<String> getTweets(String user_id) {
        return tweetCache.smembers("usertweets:" + user_id);
    }

    public static void populateTimeline() {
        Map<String, String> details = new HashMap<>();
        for (int i = 0; i < 20; i++) {
            details.clear();
            details.put("username", "ana" + i);
            details.put("id", "" + i);
            userCache.hmset("user:" + i, details);
        }

        for (int i = 1; i < 20; i++) {
            userCache.sadd("userfollowing:0", "" + i);
        }

        for (int i = 0; i < 20; i++) {
            details.clear();
            details.put("text", "ana" + i);
            details.put("id", "" + i);
            details.put("creator_id", "" + i);

            userCache.hmset("tweet:" + i, details);
            userCache.sadd("usertweets:" + i, i + "");
        }


    }


    private static boolean checkNulls(Map<String, String> map) {
        return map.containsValue(null);
    }

    private static ConcurrentMap<String,String> toConcurrentMap(Map<String, String> map) {
        String[] mapStrings = map.toString().split(",");
        CopyOnWriteArrayList<String> mapStringsConcurrent = new CopyOnWriteArrayList<>(Arrays.asList(mapStrings));
        ConcurrentMap<String,String> result = mapStringsConcurrent.parallelStream().map(hash -> braceRemover(hash).trim().split("=")).collect(Collectors.toConcurrentMap(key -> key[0], value -> value[1]));

        return result;
    }


    private static String braceRemover(String x) {
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

    public static void main(String[] args) {
//        userCache.flushAll();
//        populateTimeline();
//        getTimeline("0").forEach(System.out::println);
        System.out.println(returnUser("1").toString());
    }
}
