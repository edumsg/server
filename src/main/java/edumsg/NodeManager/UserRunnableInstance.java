package edumsg.NodeManager;

public class UserRunnableInstance extends RunnableInstance {
    public UserRunnableInstance() {
        super("user", Main.userCache);
    }
}
