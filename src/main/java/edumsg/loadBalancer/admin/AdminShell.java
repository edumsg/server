package edumsg.loadBalancer.admin;

import asg.cliche.Command;
import asg.cliche.Shell;
import asg.cliche.ShellFactory;

public class AdminShell {
    private int counter = 0;

    public static void main(String[] args) throws Exception {
        Shell shell = ShellFactory.createConsoleShell("mycli", "Welcome to My CLI", new AdminShell());
        shell.commandLoop();
    }

    @Command(description = "Increment the counter")
    public void increment(int x) {
        counter += x;
        System.out.println("Counter: " + counter);
    }

    @Command(description = "Decrement the counter")
    public void decrement() {
        counter--;
        System.out.println("Counter: " + counter);
    }
}
