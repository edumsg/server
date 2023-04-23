package edumsg.NodeManager.AppInstances;

import edumsg.NodeManager.Main;

public class DMRunnableInstance extends RunnableInstance {
    public DMRunnableInstance() {
        super("dm", Main.dmCache, DMRunnableInstance.class.getName());
    }
}
