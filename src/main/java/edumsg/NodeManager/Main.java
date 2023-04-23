package edumsg.NodeManager;

import edumsg.NodeManager.AppInstances.DMRunnableInstance;
import edumsg.NodeManager.AppInstances.ListRunnableInstance;
import edumsg.NodeManager.AppInstances.TweetRunnableInstance;
import edumsg.NodeManager.AppInstances.UserRunnableInstance;
import edumsg.NodeManager.NettyInstance.MainServerInstance;
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
        new UserRunnableInstance();
        new TweetRunnableInstance();
        new DMRunnableInstance();
        new ListRunnableInstance();
        new MainServerInstance();
    }
}
