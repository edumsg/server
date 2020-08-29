package edumsg.loadBalancer;


/*
2    * Copyright 2012 The Netty Project
3    *
4    * The Netty Project licenses this file to you under the Apache License,
5    * version 2.0 (the "License"); you may not use this file except in compliance
6    * with the License. You may obtain a copy of the License at:
7    *
8    *   http://www.apache.org/licenses/LICENSE-2.0
9    *
10   * Unless required by applicable law or agreed to in writing, software
11   * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
12   * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
13   * License for the specific language governing permissions and limitations
14   * under the License.
15   */

  import io.netty.bootstrap.Bootstrap;
  import io.netty.buffer.ByteBuf;
  import io.netty.channel.Channel;
  import io.netty.channel.EventLoopGroup;
  import io.netty.channel.nio.NioEventLoopGroup;
  import io.netty.channel.socket.nio.NioSocketChannel;
  import io.netty.handler.codec.http.*;
  import io.netty.util.CharsetUtil;
  import org.json.JSONObject;
  import java.net.URI;
  import java.nio.ByteBuffer;
  import java.util.HashMap;
  import java.util.Map;
  import java.util.UUID;


public final class HttpSnoopClient {

    private static Channel controller_ch;
    public static HashMap<String, serverInstances> serverInstances = new HashMap<>();
    private static int count = 0;

    // create HTTP request using the request body
    // set the appropriate headers for the HTTP request
    private static DefaultFullHttpRequest createReq(ByteBuf body,serverInstances server ,String App_Type) {
        URI uri = server.getUri();
        DefaultFullHttpRequest request;
        if (body == null) {
            request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1, HttpMethod.GET, uri.getRawPath());
        } else {
            request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, uri.getRawPath());
            request.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json");
            request.content().writeBytes(body);
            request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, request.content().readableBytes());
            // adjust the request header by set the micro-service instance that would handle this request
            // each time we call forward metgod to select the least response instance
            request.headers().set("App_Num", Calculation.forward(App_Type));
            request.headers().set("App_Type", App_Type);
            request.headers().set("id", UUID.randomUUID().toString());

        }
        request.setUri(uri.toString());
        request.headers().set(HttpHeaderNames.HOST, uri.getHost());
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        return request;
    }
    public static void serverCluster(ByteBuf requestContent){

        /*sendToMainServer(requestContent,serverInstances.get( (serverInstances.keySet().toArray())[ count ] ));
        System.out.println("forward to server... " + count);
        if(count == 1) {
            count = 0;
        }else {
            count = 1;
        }*/
        String host = serverInstances.entrySet().iterator().next().getValue().getHost();
        if(serverInstances.size()==1){
            sendToMainServer(requestContent,serverInstances.entrySet().iterator().next().getValue());
        }else{
            int min = (serverInstances.entrySet().iterator().next().getValue()).getTotalReq();
            for (Map.Entry<String, serverInstances> entry : serverInstances.entrySet()){
                if(entry.getValue().getTotalReq() <= min)
                    host = entry.getValue().getHost();
            }
            sendToMainServer(requestContent,serverInstances.get(host));
        }
    }
    // create channel to main server that runs on port 8080

    // create channel to controller server that runs on port 9090
    public static void ControllerChannel() {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    // this channel has different handler class that the main channel
                    .handler(new HttpControllerClientInitializer(null));

            // Make the connection attempt.
            controller_ch = b.connect("localhost", 9090).sync().channel();
            System.out.println("connection to controller server established...");


        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void sendToMainServer(ByteBuf requestContent , serverInstances server) {

        System.out.println("send to channel..."+server.getHost());
        JSONObject body = new JSONObject(requestContent.toString(CharsetUtil.UTF_8));
        String App_Type = body.getString("queue");
        DefaultFullHttpRequest request = createReq(requestContent,server, App_Type);
        server.getChannel().writeAndFlush(request);
        // after sending the request we capture an image of it to calculate statistics
        Calculation.send_time(request);
        server.setTotalReq(server.getTotalReq()+1);

    }

    // send HTTP Request to the controller server
    public static void sendToControllerServer(ByteBuffer body) {
        URI uri = URI.create(System.getProperty("uri", "http://127.17.165.225:9090/"));
        DefaultFullHttpRequest request;
        request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, uri.getRawPath());
        request.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json");
        request.content().writeBytes(body);
        request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, request.content().readableBytes());
        request.setUri(uri.toString());
        request.headers().set(HttpHeaderNames.HOST, uri.getHost());
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

        controller_ch.writeAndFlush(request);

    }
    public static void add_server_instance (String host , serverInstances server){
        serverInstances.put(host,server);
    }

    public static int getServerInstances() {
        return serverInstances.size();
    }
    public static void decrement(String host){
        serverInstances server = serverInstances.get(host);
        server.setTotalReq(server.getTotalReq() - 1);
    }
}




