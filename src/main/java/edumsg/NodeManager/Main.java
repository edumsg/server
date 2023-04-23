package edumsg.NodeManager;

import edumsg.redis.DMCache;
import edumsg.redis.ListCache;
import edumsg.redis.TweetCache;
import edumsg.redis.UserCache;

public class Main {
    public static UserCache userCache = new UserCache();
    public static DMCache dmCache = new DMCache();

    public static ListCache listCache = new ListCache();

    public static TweetCache tweetCache = new TweetCache(Main.userCache);

    public static void main(String[] args) {

    }
}
