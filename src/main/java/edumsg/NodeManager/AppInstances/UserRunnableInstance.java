package edumsg.NodeManager.AppInstances;

import edumsg.NodeManager.Main;

public class UserRunnableInstance extends RunnableInstance {
    public UserRunnableInstance() {
        super("user", Main.userCache, UserRunnableInstance.class.getName());
    }
}
