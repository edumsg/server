package edumsg.redis;

import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import edumsg.redis.UserCache;

import static edumsg.redis.UserCache.*;

public class TweetsCache extends Cache {
    public static Jedis tweetCache = redisPool.getResource();
    private static Pipeline tweetPipeline = tweetCache.pipelined();



    public static void tweetBgSave(){
        Runnable runnable = () -> {
            String res;
            res = tweetCache.bgsave();
            System.out.println(res);
        };
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(runnable, 0, 15, TimeUnit.MINUTES);
    }

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

}
