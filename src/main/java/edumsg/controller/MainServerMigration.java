package edumsg.controller;


import com.jcraft.jsch.*;

import java.io.*;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class MainServerMigration {
    private static int counter = 1;
    private boolean done = false;
    private String user;
    private String password;
    private String ip;
    private int port = 22;
    private Session session;
    private int app_num;
    private String app_type;

    public static int getCounter() {
        return counter;
    }

    public String getIp() {
        return ip;
    }

    public void print(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.length() != 0)
                System.out.println(line);
            if (line.equals("App Running Successfully!")) {
                return;
            }
        }
    }

    public void setUp(String app_type, int app_num, String correlationId, Logger log) throws IOException {
        try {
            this.app_num = app_num;
            this.app_type = app_type.toLowerCase();

            // create a session between the controller server and the remote machine
            Host host = EduMsgController.hosts.pollFirst();
            user = host.getUser();
            password = host.getPassword();
            ip = host.getIp();
            if (ip.equals("localhost")) {
                runLocally();
            } else {
                JSch jsch = new JSch();
                session = jsch.getSession(user, ip, port);
                session.setPassword(password);
                session.setConfig("StrictHostKeyChecking", "no");
                session.connect();
                System.out.println("Connection established.");
                if (!host.hasJar()) {
                    // We need to send a jar to the host
                    scpToServer();
                    install();
                }
                run();
            }
            System.out.println("DONEEE");
            host.incrementInstancesCount();
            EduMsgController.hosts.add(host);
        } catch (Exception e) {
            // TODO: 23/04/2023 handle error
            e.printStackTrace();
            //controllerHandleError(app_type, 1, "newInstance", e.getMessage() + ":failed to migrate new instance", correlationId, log);
        }
    }

    public void runLocally() throws IOException {
        String Command1 = "cd " + System.getProperty("user.dir");
        String Command2 = "java -jar target/TwitterBackend-1.0.jar " + app_type + " " + app_num;
        ProcessBuilder pb = new ProcessBuilder();

        // Set the command and arguments
        if (System.getProperty("os.name").startsWith("Windows")) {
            pb.command("cmd.exe", "/c", Command1 + " && " + Command2);
        } else {
            pb.command("bash", "-c", Command1 + " && " + Command2);
        }
        Process process = pb.start();
        print(process.getInputStream());
    }

    // open sftp channel to transfer the jar file to the remote machine
    public void scpToServer() throws Exception {
        File file = new File(System.getProperty("user.dir") + "\\newconfig.conf");
        List<String> lines = Arrays.asList("# config attributes", "instance_num = [" + app_num + "]", "instance_user = [" + user + "]", "instance_host = [" + ip + "]", "instance_pass = [" + password + "]", "main_host = [" + InetAddress.getLocalHost().getHostAddress() + "]");
        Files.write(Paths.get(file.getPath()), lines, StandardCharsets.UTF_8);
        HashMap<String, String> dbConfig = getDBConfig();
        File postgresFile = new File(System.getProperty("user.dir") + "\\newPostgres.conf");
        List<String> postgresLines = Arrays.asList("username = [" + dbConfig.get("user") + "]", "database = [" + dbConfig.get("database") + "]", "pass = [" + dbConfig.get("pass") + "]", "port = [" + dbConfig.get("port") + "]", "host = [" + InetAddress.getLocalHost().getHostAddress() + "]");
        Files.write(Paths.get(postgresFile.getPath()), postgresLines, StandardCharsets.UTF_8);
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand("cd Desktop ; mkdir Server ; cd Server ; rm -rf .[^.]* *");
        channel.setInputStream(null);
        ((ChannelExec) channel).setErrStream(System.err);
        ((ChannelExec) channel).setPty(true);
        InputStream in = channel.getInputStream();
        OutputStream out = channel.getOutputStream();
        channel.connect();
        out.write((password + "\n").getBytes());
        out.flush();
        print(in);
        channel.disconnect();
        ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
        sftpChannel.connect();
        sftpChannel.put(System.getProperty("user.dir") + "\\target\\TwitterBackend-1.0.jar", "/home/" + user + "/Desktop/Server");
        sftpChannel.put(System.getProperty("user.dir") + "\\newPostgres.conf", "/home/" + user + "/Desktop/Server");
        sftpChannel.put(System.getProperty("user.dir") + "\\logger.properties", "/home/" + user + "/Desktop/Server");
        sftpChannel.put(System.getProperty("user.dir") + "\\newConfig.conf", "/home/" + user + "/Desktop/Server");
        sftpChannel.disconnect();
        System.out.println("jar file sent...");
    }

    private HashMap<String, String> getDBConfig() throws Exception {

        String file = System.getProperty("user.dir") + "/Postgres.conf";
        java.util.List<String> lines = new ArrayList<String>();
        //extract string between square brackets and compile regex for faster performance
        Pattern pattern = Pattern.compile("\\[(.+)\\]");
        Matcher matcher;
        Exception e;
        Stream<String> stream = Files.lines(Paths.get(file));
        lines = stream.filter(line -> !line.startsWith("#")).collect(Collectors.toList());
        HashMap<String, String> map = new HashMap<>();
        //set variables based on matches
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("user")) {
                matcher = pattern.matcher(lines.get(i));
                if (matcher.find()) {
                    map.put("user", matcher.group(1));
                } else
                    throw e = new Exception("empty user in Postgres.conf");
            }
            if (lines.get(i).contains("database")) {
                matcher = pattern.matcher(lines.get(i));
                if (matcher.find())
                    map.put("database", matcher.group(1));
                else
                    throw e = new Exception("empty database name in Postgres.conf");
            }
            if (lines.get(i).contains("pass")) {
                matcher = pattern.matcher(lines.get(i));
                matcher.find();
                map.put("pass", matcher.group(1));
            }
            if (lines.get(i).contains("port")) {
                matcher = pattern.matcher(lines.get(i));
                if (matcher.find())
                    map.put("port", matcher.group(1));
                else
                    map.put("port", "5432");
            }
        }

        return map;
    }

    // install java on the remote machine
    public void install() throws IOException, JSchException, InterruptedException {
        String Command1 = "sudo apt-get -y install openjdk-19-jdk && sudo apt-get install openjdk-19-jre";
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand("sudo -S -p '' " + Command1);
        channel.setInputStream(null);
        ((ChannelExec) channel).setErrStream(System.err);
        ((ChannelExec) channel).setPty(true);
        InputStream in = channel.getInputStream();
        OutputStream out = channel.getOutputStream();
        channel.connect();
        out.write((password + "\n").getBytes());
        out.flush();
        print(in);
        channel.disconnect();
    }

    // execute commands in the remote machine terminal to run the micro-service
    public void run() throws IOException, JSchException, InterruptedException {
        String Command1 = "cd ~";
        String Command2 = "cd Desktop/Server && mv newConfig.conf config.conf && mv newPostgres.conf Postgres.conf";
        String Command3 = "java -jar TwitterBackend-1.0.jar " + app_type + " " + app_num;


        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(Command1 + "&&" + Command2 + "&&" + Command3);
        channel.setInputStream(null);
        ((ChannelExec) channel).setErrStream(System.err);
        ((ChannelExec) channel).setPty(true);
        InputStream in = channel.getInputStream();
        OutputStream out = channel.getOutputStream();
        channel.connect();
        out.write((password + "\n").getBytes());
        out.flush();
        print(in);
    }

    public void close() {
        session.disconnect();
    }
}

