package edumsg.NodeManager;

import edumsg.NodeManager.NettyInstance.MainServerInstance;

import java.io.IOException;

public class Test {

    public static void main(String[] args) throws IOException {
        Thread user = new Thread(new RunnableInstance("user", 1));
        Thread tweet = new Thread(new RunnableInstance("tweet", 1));
        Thread dm = new Thread(new RunnableInstance("dm", 1));
        Thread list = new Thread(new RunnableInstance("list", 1));
        Thread server = new Thread(new MainServerInstance(1));
        user.start();
        tweet.start();
        dm.start();
        list.start();
        server.start();
    }
}
