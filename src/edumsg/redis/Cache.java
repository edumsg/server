package edumsg.redis;

import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Cache {
    public static Jedis redisCache = new Jedis("localhost", 6379);
    private static Pipeline pipe = redisCache.pipelined();

    /////////////////////////////////////
    ////////////USER OPERATIONS//////////
    /////////////////////////////////////

    @Nullable
    public static Map<String, String> returnUser(String user_id) {
        if (redisCache.exists("user:" + user_id)) {
            return redisCache.hgetAll("user:" + user_id);
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

    public static void unFollow(String user_id, String user_being_followed_id) {
        pipe.srem("userfollowing:" + user_id, user_being_followed_id);
        pipe.srem("userfollowers:" + user_being_followed_id, user_id);
        pipe.sync();
    }

    public static void cacheFollowers(String user_id, String follower_id) {
        if ((user_id != null) && (follower_id != null)) {
            redisCache.sadd("userfollowers:" + user_id, follower_id);
        }
    }

    public static void logoutUser(String user_id) {
        redisCache.hdel("user:" + user_id, "session_id");
    }

    public static void cacheUserSession(String user_id, String session_id) {
        redisCache.hset("user:" + user_id, "session_id", session_id);
    }

    public static void mapUsernameID(String username, String id) {
        redisCache.hset("usernameid", username, id); //maps username to id
    }

    public static String returnUserID(String username) {
        return redisCache.hget("usernameid", username); //returns id based on username
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

    public static void deleteList(String list_id) {

    }

    /////////////////////////////////////
    ///////////TWEET OPERATIONS//////////
    ////////////////////////////////////

    public static void cacheTweet(String id, Map<String, String> tweetDetails) {
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
            String user = redisCache.hget("tweet:" + tweet_id, "creator_id");
            pipe.del("tweet:" + tweet_id);
            pipe.srem("usertweets:" + user, tweet_id);
            pipe.sync();
        }
    }

    public static CopyOnWriteArrayList<ConcurrentMap<ConcurrentMap<String, String>, ConcurrentMap<String, String>>> getTimeline(String user_id) {
        CopyOnWriteArrayList<ConcurrentMap<String, String>> tweets = new CopyOnWriteArrayList<>();  // Array list of tweets only
        redisCache.smembers("userfollowing:" + user_id).parallelStream()
                .forEachOrdered(user -> getTweets(user).parallelStream()
                        .forEachOrdered(tweet_id -> tweets.add(toConcurrentMap(returnTweet(tweet_id)))));

        CopyOnWriteArrayList<ConcurrentMap<ConcurrentMap<String, String>, ConcurrentMap<String, String>>> users_and_tweets = new CopyOnWriteArrayList<>();  //
        // Array list
        // of tweets only

        ConcurrentMap x = new ConcurrentHashMap<>();
        x = returnTweet("2").getFie


        ConcurrentMap<ConcurrentMap<String, String>, ConcurrentMap<String, String>> temp = new ConcurrentHashMap<>();
        ConcurrentMap<ConcurrentMap<String, String>, ConcurrentMap<String, String>> temp2 = new ConcurrentHashMap<>();

        // tweets.parallelStream().forEachOrdered(tweet_map -> );

        tweets.parallelStream().forEachOrdered(tweet_map -> {
            //System.out.println(tweet_map);
            temp.put(Cache.returnUser(tweet_map.get("creator_id")), tweet_map);
            users_and_tweets.add(temp);
            //System.out.println(Arrays.deepToString(users_and_tweets.toArray()) +"\n");
            // temp.clear();

        });

        System.out.println(Arrays.deepToString(users_and_tweets.toArray()) + "\n");


        CopyOnWriteArrayList<Integer> x = new CopyOnWriteArrayList<>();
        tweets.parallelStream().forEach(y -> x.add(1));
        System.out.println(Arrays.deepToString(x.toArray()) + "\n");

//        System.out.println(users_and_tweets.get(0).values().toString());
        return users_and_tweets;
    }

    public static Set<String> getTweets(String user_id) {
        return redisCache.smembers("usertweets:" + user_id);
    }

    public static void populateTimeline() {
        Map<String, String> details = new HashMap<>();
        for (int i = 0; i < 20; i++) {
            details.clear();
            details.put("username", "ana" + i);
            details.put("id", "" + i);
            redisCache.hmset("user:" + i, details);
        }

        for (int i = 1; i < 20; i++) {
            redisCache.sadd("userfollowing:0", "" + i);
        }

        for (int i = 0; i < 20; i++) {
            details.clear();
            details.put("text", "ana" + i);
            details.put("id", "" + i);
            details.put("creator_id", "" + i);

            redisCache.hmset("tweet:" + i, details);
            redisCache.sadd("usertweets:" + i, i + "");
        }


    }


    private static boolean checkNulls(Map<String, String> map) {
        return map.containsValue(null);
    }

    private static ConcurrentMap toConcurrentMap(Map<String, String> map) {
        String[] mapStrings = map.toString().split(",");
        CopyOnWriteArrayList<String> mapStringsConcurrent = new CopyOnWriteArrayList<>(Arrays.asList(mapStrings));
        ConcurrentMap result = mapStringsConcurrent.parallelStream().map(hash -> braceRemover(hash).split("=")).collect(Collectors.toConcurrentMap(key -> key[0], value -> value[1]));

        return result;
    }


    private static String braceRemover(String x) { // adjusts the name key in a
        // hashtable
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

}
