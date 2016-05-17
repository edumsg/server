package edumsg.tests;


import edumsg.activemq.ActiveMQConfig;
import edumsg.activemq.Producer;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActiveMQTests {
    private static class Task implements Runnable {

        private CyclicBarrier barrier;

        public Task(CyclicBarrier barrier) {
            this.barrier = barrier;
        }

        @Override
        public void run() {
            try {

                System.out.println(Thread.currentThread().getName() + " is waiting on barrier");
                barrier.await();
                System.out.println(Thread.currentThread().getName() + " has crossed the barrier");
            } catch (InterruptedException ex) {
                Logger.getLogger(ActiveMQTests.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BrokenBarrierException ex) {
                Logger.getLogger(ActiveMQTests.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void main(String args[]) {
        Producer p = new Producer(new ActiveMQConfig("TEST_QUEUE"));

        //creating CyclicBarrier for 100 thread to start simultaneously
        final CyclicBarrier cb = new CyclicBarrier(100, new Runnable(){
            @Override
            public void run(){
                //This task will be executed once all thread reaches barrier

                System.out.println("All parties are arrived at barrier, lets play");
                Logger l = Logger.getLogger(ActiveMQTests.class.getName());
                p.send("demo "+ Math.random(), ((int) Math.random())+"",l);
            }
        });

      //starting each of thread
        for(int i = 0; i<100; i++){

            new Thread(new ActiveMQTests.Task(cb), "Thread "+i).start();
        }

    }
}
