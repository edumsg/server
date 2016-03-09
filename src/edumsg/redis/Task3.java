package edumsg.redis;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Task3 implements Runnable {
    private final int sleepTime; // random sleep time for thread
    private final String taskName; // name of task
    private final static Random generator = new Random();

    // constructor
    public Task3(String name) {
        taskName = name; // set task name
// pick random sleep time between 0 and 5 seconds
        sleepTime = generator.nextInt(5000); // milliseconds
    } // end Task3 constructor

    // method run contains the code that a thread will execute
    public void run() {
        try // put thread to sleep for sleepTime amount of time
        {
            System.out.printf("%s going to sleep for %d milliseconds.\n",
                    taskName, sleepTime);
            Thread.sleep(sleepTime); // put thread to sleep
        } // end try
        catch (InterruptedException exception) {
            System.out.printf("%s %s\n", taskName,
                    "terminated prematurely due to interruption");
        } // end catch
// print task name
        System.out.printf("%s done sleeping\n", taskName);
    } // end method run


    public static void main(String[] args) {
// create and name each runnable
        Task3 task1 = new Task3("task1");
        Task3 task2 = new Task3("task2");
        Task3 task3 = new Task3("task3");
        System.out.println("Starting Executor");
// create ExecutorService to manage threads
        ExecutorService threadExecutor = Executors.newCachedThreadPool();
// start threads and place in
        threadExecutor.execute(task1);
        threadExecutor.execute(task2);
        threadExecutor.execute(task3);
// shut down worker threads when their tasks complete
        threadExecutor.shutdown();
        System.out.println("Tasks started, main ends.\n");
    }

} // end class Task3
