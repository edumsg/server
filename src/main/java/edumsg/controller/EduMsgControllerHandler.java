package edumsg.controller;

import com.jcraft.jsch.JSchException;
import edumsg.activemq.ActiveMQConfig;
import edumsg.activemq.Producer;
import edumsg.activemq.publisher;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.stomp.StompHeaders.CONTENT_LENGTH;
import static io.netty.handler.codec.stomp.StompHeaders.CONTENT_TYPE;
import static org.apache.http.HttpHeaders.CONNECTION;

public class EduMsgControllerHandler extends
        SimpleChannelInboundHandler<Object> {

    volatile String responseBody;
    Logger log = Logger.getLogger(EduMsgController.class.getName());
    ExecutorService executorService = Executors.newCachedThreadPool();
    private HttpRequest request;
    private String requestBody;
    private String correlationId;

    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
        ctx.writeAndFlush(response);
    }

    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    // read incoming requests
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

        correlationId = String.valueOf(System.currentTimeMillis());
        if (msg instanceof HttpRequest) {
            request = (HttpRequest) msg;
            if (HttpHeaders.is100ContinueExpected(request)) {
                send100Continue(ctx);
            } }

        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;
            ByteBuf content = httpContent.content();
            setRequestBody(content.toString(CharsetUtil.UTF_8));
        }

        if (msg instanceof LastHttpContent) {
            LastHttpContent trailer = (LastHttpContent) msg;
            writeResponse(trailer, ctx);
        }
    }

    private synchronized void writeResponse(HttpObject currentObj, final ChannelHandlerContext ctx) throws
            NumberFormatException, IOException, InterruptedException, JSONException, ExecutionException, JSchException {

        //decide the behaviour of the controller server according to the incoming command
        JSONObject requestJson = new JSONObject(requestBody);
        // extract info from the request body
        String command = requestJson.getString("command");
        String app_type = requestJson.getString("app_type");
        int app_num = requestJson.getInt("app_num");
        String Queue = (requestJson.getString("app_type")).toUpperCase() + "_" + app_num;

        System.out.println("request body... " + requestBody);
        notifier notifier = new notifier(this, Queue);

        // the command to create new micro-service instance
        if (command.equals("newInstance")) {
            Runnable r = new Runnable() {
                public void run() {
                    try {
                        serviceMigration newInstance = new serviceMigration();
                        newInstance.setUp(app_type, app_num, correlationId, log);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            executorService.submit(r);
        } else {
            // the command to update update class version in all running micro-service instance
            if (command.equals("updateClass")) {
                publisher topic = new publisher(new ActiveMQConfig(app_type));
                // steps to handle update command in controller-side
                UpdateVersion.Update(requestBody, correlationId, log);
                // publish in the app topic that new class version available for all instances of this app
                topic.publish(requestBody);
            } else {
                // normal command handling routine
                sendMessageToActiveMQ(requestBody, Queue);
            }
        }
        System.out.println("waiting...");

        Future future = executorService.submit(notifier);
        this.responseBody = (String) future.get();
        System.out.println("-----------");

        JSONObject json = new JSONObject(responseBody);
        HttpResponseStatus status = null;
        if (!json.has("message"))

            status = new HttpResponseStatus(200,
                    "ok");
        else {
            status = new HttpResponseStatus(Integer.parseInt((String) json
                    .get("code")), (String) json.get("message"));
        }

        boolean keepAlive = HttpHeaders.isKeepAlive(request);
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
                status, Unpooled.copiedBuffer(responseBody, CharsetUtil.UTF_8));

        response.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
        if (keepAlive) {
            response.headers().set(CONTENT_LENGTH,
                    response.content().readableBytes());
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        ctx.writeAndFlush(response);
        System.out.println("res..." + responseBody);
    }

    private void sendMessageToActiveMQ(String jsonBody, String queue) {
        Producer p = new Producer(new ActiveMQConfig(queue + "_CONTROLLER.INQUEUE"));
        p.send(jsonBody, correlationId + "", log);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Logger getLog() {
        return log;
    }
}

