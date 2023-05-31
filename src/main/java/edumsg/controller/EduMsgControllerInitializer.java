package edumsg.controller;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.ssl.SslContext;

public class EduMsgControllerInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;

    public EduMsgControllerInitializer(SslContext sslCtx) {
        this.sslCtx = sslCtx;
    }

    @Override
    protected void initChannel(SocketChannel arg0) {

        CorsConfig corsConfig = CorsConfig.withAnyOrigin().allowedRequestHeaders("X-Requested-With", "Content-Type",
                "Content-Length").allowedRequestMethods(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod
                .DELETE, HttpMethod.OPTIONS).build();

        ChannelPipeline p = arg0.pipeline();
        if (sslCtx != null) {
            p.addLast(sslCtx.newHandler(arg0.alloc()));
        }
        p.addLast("decoder", new HttpRequestDecoder());
        p.addLast("encoder", new HttpResponseEncoder());
        p.addLast(new CorsHandler(corsConfig));
        p.addLast(new EduMsgControllerHandler());
    }

}
