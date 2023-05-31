package edumsg.NodeManager.NettyInstance;

import edumsg.activemq.ActiveMQConfig;
import edumsg.activemq.Producer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class MainServerHandler extends
        SimpleChannelInboundHandler<Object> {
    Logger log = Logger.getLogger(MainServerInstance.class.getName());
    ExecutorService executorService = Executors.newCachedThreadPool();
    private HttpRequest request;
    private String requestBody;
    private String correlationId;
    private String responseBody;
    private int cur_instance;

    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
                CONTINUE);
        ctx.writeAndFlush(response);
    }

    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg)
            throws Exception {

        if (msg instanceof HttpRequest) {
            requestBody = "";
            HttpRequest request = this.request = (HttpRequest) msg;
            correlationId = request.headers().get("id");
            cur_instance = request.headers().getInt("App_Num");
            if (HttpHeaders.is100ContinueExpected(request)) {
                send100Continue(ctx);
            }
        }

        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;
            ByteBuf content = httpContent.content();
            requestBody = requestBody + content.toString(CharsetUtil.UTF_8);
        }

        if (msg instanceof LastHttpContent) {
            LastHttpContent trailer = (LastHttpContent) msg;
            writeResponse(trailer, ctx);
        }
    }

    private synchronized void writeResponse(HttpObject currentObj, final ChannelHandlerContext ctx) throws
            NumberFormatException, InterruptedException, JSONException, ExecutionException {


        JSONObject requestJson = new JSONObject(requestBody);
        // get the name of intended queue where we will direct this request
        String Queue = requestJson.getString("queue") + "_" + cur_instance;
        MainServerNotifier notifier = new MainServerNotifier(this, Queue);
        System.out.println("Request Body: " + requestBody);
        sendMessageToActiveMQ(requestBody, Queue);

        System.out.println("waiting...");
        // assign a thread to handle this request from the executors pool
        Future future = executorService.submit(notifier);
        this.responseBody = (String) future.get();

        System.out.println("res..." + getResponseBody());
        // process to send the response back through its channel
        JSONObject json = new JSONObject(getResponseBody());
        HttpResponseStatus status = null;
        if (!json.has("message"))
            status = new HttpResponseStatus(Integer.parseInt((String) json
                    .get("code")),
                    Integer.parseInt((String) json.get("code")) == 200 ? "Ok"
                            : "Bad Request");
        else {
            status = new HttpResponseStatus(Integer.parseInt((String) json
                    .get("code")), (String) json.get("message"));
        }
        boolean keepAlive = HttpHeaders.isKeepAlive(request);

        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
                status, Unpooled.copiedBuffer(responseBody, CharsetUtil.UTF_8));
        // set some info at the headers of the responses to be used in the load balancer
        response.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().setInt("App_Num", cur_instance);
        response.headers().set("App_Type", requestJson.getString("queue"));
        response.headers().set("id", getCorrelationId());
        if (keepAlive) {
            response.headers().set(CONTENT_LENGTH,
                    response.content().readableBytes());
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }
        ctx.writeAndFlush(response);
    }

    private void sendMessageToActiveMQ(String jsonBody, String queue) {
        Producer p = new Producer(new ActiveMQConfig(queue.toUpperCase() + ".INQUEUE"));
        p.send(jsonBody, correlationId, log);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setRequestBody(String requestBody) {

        this.requestBody = requestBody;
    }

    public String getCorrelationId() {

        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
