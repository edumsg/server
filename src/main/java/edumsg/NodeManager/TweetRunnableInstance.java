package edumsg.NodeManager;

public class TweetRunnableInstance extends RunnableInstance {
    public TweetRunnableInstance() {
        super("tweet", Main.tweetCache);
    }
}
