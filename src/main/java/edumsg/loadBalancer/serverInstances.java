package edumsg.loadBalancer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.URI;

public class serverInstances {
    private Channel channel;
    private String host;
    private URI uri;
    private int totalReq;

    public serverInstances (String host){
        this.host = host;
        this.uri = URI.create(System.getProperty("uri", "http://"+host+":8080/"));
        this.creat_channel();
    }
    public void creat_channel(){
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new HttpSnoopClientInitializer(null));

            // Make the connection attempt.
            channel = b.connect(host, 8080).sync().channel();
            System.out.println("Channel established...");


        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public int getTotalReq() {
        return totalReq;
    }

    public void setTotalReq(int totalReq) {
        this.totalReq = totalReq;
    }

    public String getHost() {
        return host;
    }

    public Channel getChannel() {
        return channel;
    }

    public URI getUri() {
        return uri;
    }
}
