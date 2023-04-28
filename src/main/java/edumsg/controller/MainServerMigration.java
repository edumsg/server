package edumsg.controller;


import com.jcraft.jsch.*;
import org.zeroturnaround.zip.ZipUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;


public class MainServerMigration {
    private static int counter = 1;
    private String user;
    private String password;
    private String ip;
    private int port = 22;
    private Session session;
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
        MainServerMigration.counter = counter;
    }

    public void setUp(int cur_instance, String correlationId, Logger log) throws IOException {
        try {
            instance = cur_instance;
            server_props();     // get the info about the remote machine which will run the micro-service
            // create a session between the controller server and the remote machine
            JSch jsch = new JSch();
            session = jsch.getSession(user, ip, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            System.out.println("Connection established.");
            // steps to migrate the micro-service to the remote machine
            zipping();
            scpToServer();
            unzipCommand();
            install();
            run();
            session.disconnect();
            System.out.println(" session disconnected...");
        } catch (Exception e) {
            // TODO: 23/04/2023 handle error
            //controllerHandleError(app_type, 1, "newInstance", e.getMessage() + ":failed to migrate new instance", correlationId, log);
        }
    }

    public void zipping() throws IOException {

        // write configuration file for the migrated the micro-service and put it in the package containing the source-code
        File file = new File(System.getProperty("user.dir") + "\\config.conf");
        List<String> lines = Arrays.asList("# config attributes", "instance_num = [" + instance + "]", "instance_user = [" + user + "]", "instance_host = [" + ip + "]", "instance_pass = [" + password + "]");
        Files.write(Paths.get(file.getPath()), lines, StandardCharsets.UTF_8);

        ZipUtil.pack(new File(System.getProperty("user.dir")), new File(System.getProperty("user.dir") + ".zip"));
        System.out.println("zipped successfully");

    }

    // open sftp channel to transfer the zip file to the remote machine
    public void scpToServer() throws JSchException, SftpException, IOException {
        //Creating and clearing a directory for the code
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
        byte[] tmp = new byte[1024];
        print(in, tmp, channel);
        channel.disconnect();
        ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
        sftpChannel.connect();
        sftpChannel.put(System.getProperty("user.dir") + ".zip", "/home/" + user + "/Desktop/Server");
        sftpChannel.disconnect();
        System.out.println("package sent...");
    }

    // open exec channel to control the terminal on the remote machine
    public void unzipCommand() throws IOException, JSchException {
        String Command1 = "sudo apt-get install -y unzip";
        String Command2 = "cd Desktop ; cd Server";
        String Command3 = "unzip server.zip";

        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand("sudo -S -p '' " + Command1 + ";" + Command2 + ";" + Command3);
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

    // install all required packages and libraries on the remote machine
    public void install() throws IOException, JSchException {
        String Command1 = "sudo apt-get -y install openjdk-8-jdk";
        String Command2 = "docker run -p 61616:61616 -p 8161:8161 rmohr/activemq";
        String Command3 = "docker run -p 6379:6379 redis";

        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand("sudo -S -p '' " + Command1 + "&&" + Command2 + "&&" + Command3);
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
    public void run() throws IOException, JSchException {
        String Command1 = "cd ~";
        String Command2 = "cd Desktop/Server";
        String Command3 = "mvn exec:java -Dexec.mainClass=\"edumsg.NodeManager.Main\"";


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

