package edumsg.controller;


import com.jcraft.jsch.*;

import java.io.*;
import java.util.Properties;
import java.util.logging.Logger;


public class ServiceMigration {
    private static int counter = 1;
    private String user;
    private String password;
    private String ip;
    private int port = 22;
    private Session session;
    private String app;
    private int instance;

    public static void print(InputStream in, byte[] tmp, Channel channel) throws IOException {
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0) break;
                System.out.print(new String(tmp, 0, i));
            }
            if (channel.isClosed()) {
                System.out.println("exit-status: " + channel.getExitStatus());
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (Exception ignored) {
            }
        }
    }

    public static int getCounter() {
        return counter;
    }

    public static void setCounter(int counter) {
        ServiceMigration.counter = counter;
    }

    public void setUp(String app_type, int cur_instance, String correlationId, Logger log) throws IOException {
        try {
            app = app_type;
            instance = cur_instance;
            server_props();     // get the info about the remote machine which will run the micro-service
            // create a session between the controller server and the remote machine
            JSch jsch = new JSch();
            session = jsch.getSession(user, ip, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            System.out.println("Connection established.");
            String run_class;
            switch (app_type.toUpperCase()) {
                case "USER":
                    run_class = "UserMain";
                    break;
                case "DM":
                    run_class = "DMMain";
                    break;
                case "TWEET":
                    run_class = "TweetMain";
                    break;
                case "LIST":
                    run_class = "ListMain";
                    break;
                case "SERVER":
                    run_class = "EduMsgNettyServer";
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + app_type.toUpperCase());
            }
            // steps to migrate the micro-service to the remote machine
            //zipping();
            scpToServer();
            //unzipCommand();
            install();
            run(run_class);
            session.disconnect();
            System.out.println(" session discounected...");
        } catch (Exception e) {
            // TODO: 23/04/2023 handle error 
            //controllerHandleError(app_type, 1, "newInstance", e.getMessage() + ":failed to migrate new instance", correlationId, log);
        }
    }


    // open sftp channel to transfer the zip file to the remote machine
    public void scpToServer() throws JSchException, SftpException {

        ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
        sftpChannel.connect();
        sftpChannel.put("C:\\Users\\OS\\Desktop\\Edumsg-comp\\Micro-services\\" + app + ".zip", "/home/" + user + "/Desktop");
        sftpChannel.disconnect();
        System.out.println("package sent...");
    }

   
    // install all required packages and libraries on the remote machine
    public void install() throws IOException, JSchException {
        String Command1 = "sudo apt-get -y install openjdk-8-jdk";
        String Command2 = "sudo apt install redis-tools -y";

        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand("sudo -S -p '' " + Command1 + "&&" + Command2);
        channel.setInputStream(null);
        ((ChannelExec) channel).setErrStream(System.err);
        ((ChannelExec) channel).setPty(true);
        InputStream in = channel.getInputStream();
        OutputStream out = channel.getOutputStream();
        channel.connect();
        out.write((password + "\n").getBytes());
        out.flush();
        byte[] tmp = new byte[1024];
        print(in, tmp, channel);
        channel.disconnect();

    }

    // execute commands in the remote machine terminal to run the micro-service
    public void run(String run_class) throws IOException, JSchException {
        String Command1 = "cd ~";
        String Command2 = "cd Desktop/" + app.toLowerCase() + "/src/main/java";
        String Command3 = "javac -cp .:./jars/* edumsg/*/*.java";
        String Command4;
        if (run_class.equals("EduMsgNettyServer")) {
            Command4 = "java -cp .:./jars/* edumsg/netty/" + run_class;
        } else {
            Command4 = "java -cp .:./jars/* edumsg/shared/" + run_class;
        }


        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(Command1 + "&&" + Command2 + "&&" + Command3 + "&&" + Command4);
        channel.setInputStream(null);
        ((ChannelExec) channel).setErrStream(System.err);
        ((ChannelExec) channel).setPty(true);
        InputStream in = channel.getInputStream();
        OutputStream out = channel.getOutputStream();
        channel.connect();
        out.write((password + "\n").getBytes());
        out.flush();
        byte[] tmp = new byte[1024];
        print(in, tmp, channel);
        channel.disconnect();

    }

    // get the access info (ip,user, and password) to the server node we will migrate the micro-service to.
    // we have a config file containing the access info for the nodes cluster
    public void server_props() throws IOException {
        File configFile = new File("IPs.properties");

        FileReader reader = new FileReader(configFile);
        Properties props = new Properties();
        props.load(reader);

        ip = props.getProperty("ip" + getCounter());
        user = props.getProperty("user" + getCounter());
        password = props.getProperty("password" + getCounter());
        reader.close();
        setCounter(getCounter() + 1);

    }

    public void close() {
        session.disconnect();
    }
}

