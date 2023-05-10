package edumsg.NodeManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Scanner;

public class Test {
    public static LinkedList<Process> processes = new LinkedList<>();
    public static LinkedList<Thread> threads = new LinkedList<>();

    public static void main(String[] args) throws IOException {
        run("user", 1);
        run("tweet", 1);
        run("dm", 1);
        run("list", 1);
        run("server", 1);

        Scanner sc = new Scanner(System.in);
        while (true) {
            String s = sc.next();
            if (s.equals("exit")) {
                break;
            }
        }
        for (Process p : processes) {
            try {
                int exitCode = p.waitFor();
                System.out.println("Exited with error code " + exitCode);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            p.destroy();
        }
    }

    public static void run(String app, int num) {
        String command1 = "cd " + System.getProperty("user.dir");
        String command2 = "java -jar target/TwitterBackend-1.0.jar " + app + " " + num;
        String command = command1 + " && " + command2;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                ProcessBuilder pb = new ProcessBuilder();
                if (System.getProperty("os.name").startsWith("Windows")) {
                    pb.command("cmd.exe", "/c", command);
                } else {
                    pb.command("bash", "-c", command);
                }
                try {
                    Process process = pb.start();
                    processes.add(process);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line != null)
                            System.out.println("(" + app + "_" + num + ")" + line);

                    }
//                    try {
//                        int exitCode = process.waitFor();
//                        System.out.println("Exited with error code " + exitCode);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        Thread thread = new Thread(r);
        thread.start();
        threads.add(thread);
    }
}
