package edumsg.loadBalancer;

/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.util.CharsetUtil;

import java.util.HashMap;

import static java.lang.System.currentTimeMillis;


public class HttpSnoopClientHandler extends SimpleChannelInboundHandler<HttpObject> {

    private static String response;
    private static HashMap<String, notifier> notifiers = new HashMap<>();
    private notifier notifier;
    private String reqId;

    public static String getRes() {
        return response;
    }

    public static void setRes(String res) {
        HttpSnoopClientHandler.response = res;
    }

    public static void add_notifier(String reqId, notifier notifier) {
        notifiers.put(reqId, notifier);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {

        if (msg instanceof DefaultHttpResponse) {

            // clear the previous response
            response = "";
            reqId = ((DefaultHttpResponse) msg).headers().get("id");
            Calculation.reveive_time((DefaultHttpResponse) msg, currentTimeMillis());
            // after receiving a response decrement the number of incomplete (concurrent) requests for a specific server instance
            String remoteAdress = ctx.channel().remoteAddress().toString().split(":")[0].substring(1);
            //System.out.println(remoteAdress);
            HttpSnoopClient.decrement(remoteAdress);
        }
        // in case of very long sequence of bytes in the response the response will be divided between HttpContent buffer & DefaultLastHttpContent buffer
        if (msg instanceof HttpContent && !(msg instanceof DefaultLastHttpContent)) {
            HttpContent httpContent = (HttpContent) msg;
            ByteBuf content = httpContent.content();
            response = content.toString(CharsetUtil.UTF_8);
        }

        if (msg instanceof DefaultLastHttpContent) {

            DefaultLastHttpContent HttpLastContent = (DefaultLastHttpContent) msg;
            ByteBuf lastContent = HttpLastContent.content();
            response = response + lastContent.toString(CharsetUtil.UTF_8);
            // release the thread that wait for the response to be completed
            notifier = notifiers.get(reqId);
            notifier.setResponse(response);
            notifier.getLatch().countDown();
        }


    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}

