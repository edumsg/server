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
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.json.JSONObject;

import static edumsg.loadBalancer.HttpSnoopClient.getServerInstances;
import static java.lang.System.currentTimeMillis;


public class HttpControllerClientHandler extends SimpleChannelInboundHandler<HttpObject> {

    private static String response;

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {

        if (msg instanceof DefaultHttpResponse) {

            // clear the previous response
            response = "";
        }

        if (msg instanceof HttpContent && !(msg instanceof DefaultLastHttpContent)) {
            HttpContent httpContent = (HttpContent) msg;
            ByteBuf content = httpContent.content();
            response = content.toString(CharsetUtil.UTF_8);
        }

        if (msg instanceof DefaultLastHttpContent) {

            DefaultLastHttpContent HttpLastContent = (DefaultLastHttpContent)msg;
            ByteBuf lastContent = HttpLastContent.content();
            response =response + lastContent.toString(CharsetUtil.UTF_8);
            systemStatus.latch.countDown();
            System.out.println("res in the controller channel..." + response);
            JSONObject resJson = new JSONObject(response);
            if(resJson.getString("code").equals("200")){
                switch (resJson.getString("command")){
                    case "newInstance" :
                        Calculation.new_instance(resJson.getString("app").split("_")[0],resJson.getString("msg"));
                        System.out.println("instances after..."+getServerInstances());
                        break;
                    case "start":
                        Calculation.reflect_command(resJson.getString("app_type"),true);
                        break;
                    case "stop" :
                        Calculation.reflect_command(resJson.getString("app_type"),false);
                        break;

                }
            }
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }



    public static String getRes() {
        return response;
    }

}


