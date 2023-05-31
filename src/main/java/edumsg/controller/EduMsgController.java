package edumsg.controller;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Properties;
import java.util.TreeSet;

public class EduMsgController {
    static final boolean SSL = System.getProperty("ssl") != null;

    static final int PORT = getPort();
    public static HashMap<String, Host> hostMap = new HashMap<>();
    static TreeSet<Host> hosts;

    static {
        try {
            hosts = initializeHosts();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static TreeSet<Host> initializeHosts() throws IOException {
        TreeSet<Host> hosts = new TreeSet<>();
        File configFile = new File("IPs.properties");
        FileReader reader = new FileReader(configFile);
        Properties props = new Properties();
        props.load(reader);
        int i = 1;
        while (props.getProperty("ip" + i) != null) {
            String ip = props.getProperty("ip" + i);
            String user = props.getProperty("user" + i);
            String password = props.getProperty("password" + i);
            Host host = new Host(ip, user, password);
            hosts.add(host);
            hostMap.put(ip, host);
            i++;
        }
        Host host = new Host("localhost", null, null);
        //Loadbalancer + Controller + Netty server + 4 apps
        host.setInstancesCount(7);
        hosts.add(host);
        hostMap.put("localhost", host);
        return hosts;
    }

    public static void main(String[] args) throws Exception {
        getHostDetails();
        Logger log = Logger.getLogger(EduMsgController.class);
        final SslContext sslCtx;
        if (SSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }

        // Configure the server.
        EventLoopGroup acceptorGroup = new NioEventLoopGroup(1);
        EventLoopGroup handlerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bs = new ServerBootstrap();
            bs.group(acceptorGroup, handlerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new EduMsgControllerInitializer(sslCtx));
            Channel ch = bs.bind(PORT).sync().channel();

            System.err.println("Server is listening on "
                    + (SSL ? "https" : "http") + "://127.0.0.1:" + PORT + '/');
            ch.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Controller is not running");
        }
    }

    private static int getPort() {
        int PORT;
        try {
            PORT = Integer.parseInt(System.getenv("PORT"));
        } catch (Exception e) {

            PORT = Integer.parseInt(System.getProperty("port", SSL ? "8443" : "9090"));
        }
        return PORT;
    }

    public static void getHostDetails() {
        InetAddress localHost;
        try {
            localHost = InetAddress.getLocalHost();
            System.err.println("Your current IP address : " + localHost.getHostAddress());
            System.err.println("Your current Hostname : " + localHost.getHostName());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
