package edumsg.redis;

import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Pipeline;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class TweetCache extends Cache {

    private Pipeline tweetPipeline = jedisCache.pipelined();
    private UserCache userCache;

    public TweetCache(UserCache userCache) {
        this.userCache = userCache;
    }

    //method used to test timeline generation
    public void populateTimeline() {
        Map<String, String> details = new HashMap<>();
        for (int i = 0; i < 20; i++) {
            details.clear();
            details.put("username", "ana" + i);
            details.put("id", "" + i);
            userCache.jedisCache.hmset("user:" + i, details);
        }

        for (int i = 1; i < 20; i++) {
            userCache.jedisCache.sadd("userfollowing:0", "" + i);
        }

        for (int i = 0; i < 20; i++) {
            details.clear();
            details.put("text", "ana" + i);
            details.put("id", "" + i);
            details.put("creator_id", "" + i);

            userCache.jedisCache.hmset("tweet:" + i, details);
            userCache.jedisCache.sadd("usertweets:" + i, i + "");
        }


    }

    @Nullable
    public Map<String, String> returnTweet(String id) {
        if (jedisCache.exists("tweet:" + id)) {
            return jedisCache.hgetAll("tweet:" + id);
        } else {
            return null;
        }

    }

    //pipeline allows execution of multiple operations in 1 request to Redis saving network latencies
    public void deleteTweet(String tweet_id) {
        if (tweet_id != null) {
            String user = jedisCache.hget("tweet:" + tweet_id, "creator_id");
            tweetPipeline.del("tweet:" + tweet_id);
            tweetPipeline.srem("usertweets:" + user, tweet_id);
            tweetPipeline.sync();
        }
    }

    //generates timeline
    public CopyOnWriteArrayList<ConcurrentMap<ConcurrentMap<String, String>, ConcurrentMap<String, String>>> getTimeline(String user_id) {
        CopyOnWriteArrayList<ConcurrentMap<String, String>> tweets = new CopyOnWriteArrayList<>();  // Array list of tweets only


        //parallel stream used as this was designed to handle Redis' max limit of 4 billion entries per data set
        jedisCache.smembers("userfollowing:" + user_id).parallelStream()
                .forEachOrdered(user -> getTweets(user).parallelStream()
                        .forEachOrdered(tweet_id -> tweets.add(toConcurrentMap(returnTweet(tweet_id)))));

        //special type of ArrayList that is thread safe
        CopyOnWriteArrayList<ConcurrentMap<ConcurrentMap<String, String>, ConcurrentMap<String, String>>> users_and_tweets = new CopyOnWriteArrayList<>();  // Array list of user and tweets map

        //tweets with their users added to COW Array List in a HashMap
        tweets.parallelStream().forEachOrdered(tweet_map -> {
            users_and_tweets.add(new ConcurrentHashMap() {{
                put(tweet_map, toConcurrentMap(userCache.returnUser(tweet_map.get("creator_id"))));
            }});
        });
        return users_and_tweets;
    }

    public Set<String> getTweets(String user_id) {
        return jedisCache.smembers("usertweets:" + user_id);
    }

    public void cacheTweet(String id, Map<String, String> tweetDetails) {
        if (!Cache.checkNulls(tweetDetails)) {
            jedisCache.hmset("tweet:" + id, tweetDetails);
        }
    }

}
