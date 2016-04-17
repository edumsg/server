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

package edumsg.netty;

import edumsg.activemq.ActiveMQConfig;
import edumsg.activemq.Producer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jms.JMSException;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class EduMsgNettyServerHandler extends
        SimpleChannelInboundHandler<Object> {

    private HttpRequest request;
    private String requestBody;
    private volatile String responseBody;
    Logger log = Logger.getLogger(EduMsgNettyServer.class.getName());
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg)
            throws Exception {

        if (msg instanceof HttpRequest) {
            HttpRequest request = this.request = (HttpRequest) msg;
            if (HttpHeaders.is100ContinueExpected(request)) {
                send100Continue(ctx);
            }

        }
        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;
             ByteBuf content = httpContent.content();
            setRequestBody(content.toString(CharsetUtil.UTF_8));
        }
        if (msg instanceof LastHttpContent) {
            LastHttpContent trailer = (LastHttpContent) msg;
            writeresponse(trailer, ctx);
        }
    }

    private void writeresponse(HttpObject currentObj,
                               final ChannelHandlerContext ctx) throws JMSException,
            NumberFormatException, JsonParseException, JsonMappingException,
            IOException, InterruptedException {
        try {
            JSONObject requestJson = new JSONObject(requestBody);
            NettyNotifier notifier = new NettyNotifier(this, requestJson.getString("queue"));
            notifier.start();
            sendMessageToActiveMQ(requestBody, requestJson.getString("queue"));

            synchronized (this) {
                System.out.println("waited");
                this.wait();
            }
            System.out.println("notified");
            System.out.println("netty" + getResponseBody());
            System.out.println("-----------");
            JSONObject json = new JSONObject(getResponseBody());
            HttpResponseStatus status = null;
            if (!json.has("message"))
                status = new HttpResponseStatus(Integer.parseInt((String) json
                        .get("code")),
                        Integer.parseInt((String) json.get("code")) == 200 ? "Ok"
                                : "Bad Request");
            else
                status = new HttpResponseStatus(Integer.parseInt((String) json
                        .get("code")), (String) json.get("message"));

            boolean keepAlive = HttpHeaders.isKeepAlive(request);
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
                    status, Unpooled.copiedBuffer(responseBody, CharsetUtil.UTF_8));

            response.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
            if (keepAlive) {
                response.headers().set(CONTENT_LENGTH,
                        response.content().readableBytes());
                response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            }

            String cookieString = request.headers().get(COOKIE);
            if (cookieString != null) {
                Set<Cookie> cookies = CookieDecoder.decode(cookieString);
                if (!cookies.isEmpty()) {
                    for (Cookie cookie : cookies) {
                        response.headers().add(SET_COOKIE,
                                ServerCookieEncoder.encode(cookie));
                    }
                }
            }
            // ChannelFuture f = ctx.channel().write(response);
            // if (response.getStatus().code() != 200)
            // f.addListener(ChannelFutureListener.CLOSE);
            ctx.write(response);
            channelReadComplete(ctx);
        }
        catch (JSONException e)
        {
            if (requestBody != null)
                System.out.println(requestBody);
            e.printStackTrace();
        }
    }

    private void sendMessageToActiveMQ(String jsonBody, String queue) {
        Producer p = new Producer(new ActiveMQConfig(queue.toUpperCase() + ".INQUEUE"));
        p.send(jsonBody, "1", log);
    }

    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
                CONTINUE);
        ctx.write(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }
}
