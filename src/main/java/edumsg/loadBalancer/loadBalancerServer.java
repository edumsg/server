package edumsg.loadBalancer;

/*
EduMsg is made available under the OSI-approved MIT license.
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
IN THE SOFTWARE.
*/

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
import java.net.InetAddress;
import java.net.UnknownHostException;

public class loadBalancerServer {
    static final boolean SSL = System.getProperty("ssl") != null;

    static final int PORT = getPort();

    public static void main(String[] args) throws Exception {
        getHostDetails();
        Logger log = Logger.getLogger(edumsg.loadBalancer.loadBalancerServer.class);
        final SslContext sslCtx;
        if (SSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx =  SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
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
                    .childHandler(new loadBalancerServerInitializer(sslCtx));
            Channel ch = b.bind(PORT).sync().channel();

            System.err.println("Server is listening on "
                    + (SSL ? "https" : "http") + "://127.0.0.1:" + PORT + '/');
            // set up the channels to connect to main server and controller server
            HttpSnoopClient.ControllerChannel();
            Calculation.initial_instances();
            systemStatus.send();
            ch.closeFuture().sync();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println("Server is not running");
        }
        finally {
        }
    }

    private static int getPort() {
        int PORT;
        try {
            PORT = Integer.parseInt(System.getenv("PORT"));
        } catch ( Exception e ) {
            PORT = Integer.parseInt(System.getProperty("port", SSL ? "8443" : "7070"));
        }
        return PORT;
    }

    public static void getHostDetails () {
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

