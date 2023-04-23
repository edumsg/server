package edumsg.NodeManager.AppInstances;

import edumsg.NodeManager.Main;

public class TweetRunnableInstance extends RunnableInstance {
    public TweetRunnableInstance() {
        super("tweet", Main.tweetCache, TweetRunnableInstance.class.getName());
    }
}
