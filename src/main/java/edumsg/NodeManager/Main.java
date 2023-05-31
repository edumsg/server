package edumsg.NodeManager;

import edumsg.NodeManager.NettyInstance.MainServerInstance;
import edumsg.redis.*;

import java.util.HashMap;

public class Main {
    public static HashMap<String, Cache> cacheMap = new HashMap<>();

    static {
        cacheMap.put("user", new UserCache());
        cacheMap.put("dm", new DMCache());
        cacheMap.put("list", new ListCache());
        cacheMap.put("tweet", new TweetCache((UserCache) cacheMap.get("user")));
    }

    public static void main(String[] args) {
        if (args[0].equals("server")) {
            MainServerInstance server = new MainServerInstance(Integer.parseInt(args[1]));
            server.run();
        } else {
            RunnableInstance app = new RunnableInstance(args[0], Integer.parseInt(args[1]));
            app.run();
        }
    }
}
