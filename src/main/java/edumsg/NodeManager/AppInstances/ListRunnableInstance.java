package edumsg.NodeManager.AppInstances;

import edumsg.NodeManager.Main;

public class ListRunnableInstance extends RunnableInstance {
    public ListRunnableInstance() {
        super("list", Main.listCache, ListRunnableInstance.class.getName());
    }
}
