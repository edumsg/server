package edumsg.NodeManager.NettyInstance;

import edumsg.redis.EduMsgRedis;
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

import javax.net.ssl.SSLException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;

public class MainServerInstance implements Runnable {
    final boolean SSL = System.getProperty("ssl") != null;

    final int PORT = getPort();
    private final int cur_instance;

    public MainServerInstance(int currentInstance) {
        this.cur_instance = currentInstance;
    }

    private int getPort() {
        int PORT;
        try {
            PORT = Integer.parseInt(System.getenv("PORT"));
        } catch (Exception e) {
            PORT = Integer.parseInt(System.getProperty("port", SSL ? "8443" : "8080"));
        }
        return PORT;
    }

    public void getHostDetails() {
        InetAddress localHost;
        try {
            localHost = InetAddress.getLocalHost();
            System.err.println("Your current IP address : " + localHost.getHostAddress());
            System.err.println("Your current Hostname : " + localHost.getHostName());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        getHostDetails();
        Logger log = Logger.getLogger(MainServerInstance.class);
        // Configure SSL.
        EduMsgRedis.redisCache.flushDB();
//        EduMsgRedis.bgSave();
        final SslContext sslCtx;
        if (SSL) {
            SelfSignedCertificate ssc = null;
            try {
                ssc = new SelfSignedCertificate();
                sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            } catch (CertificateException | SSLException e) {
                throw new RuntimeException(e);
            }
        } else {
            sslCtx = null;
        }

        // Configure the server.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new MainServerInitializer(sslCtx));
//            b.option(ChannelOption.SO_KEEPALIVE, true);
//           System.out.println(PORT);
            Channel ch = b.bind(PORT).sync().channel();

            System.err.println("Server is listening on "
                    + (SSL ? "https" : "http") + "://127.0.0.1:" + PORT + '/');
            System.out.println("App Running Successfully!");

            ch.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Server is not running");
        } finally {
//            bossGroup.shutdownGracefully();
//            workerGroup.shutdownGracefully();
        }
    }
}
