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
import edumsg.activemq.Consumer;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.util.concurrent.Callable;

public class NettyNotifier implements Callable<String> {

    private EduMsgNettyServerHandler serverHandler;
    private String responseBody;
    private String queueName;
    private static ActiveMQConfig activeMQConfig;
    private static Consumer consumer;

    public NettyNotifier(EduMsgNettyServerHandler serverHandler,
                         String queueName) {
        this.serverHandler = serverHandler;
        this.setQueueName(queueName);
    }

    @Override
    public String call() {
        try {
//            if (activeMQConfig == null || !activeMQConfig.getQueueName().contains(getQueueName()))
//            {
                activeMQConfig = new ActiveMQConfig(getQueueName()
                        .toUpperCase() + ".OUTQUEUE");
                consumer = new Consumer(activeMQConfig, serverHandler.getCorrelationId());
//            }
//            consumer.getConn().start();
            Message message = consumer.getConsumer().receive();
            String msgTxt = ((TextMessage) message).getText();
            System.out.println("message " + msgTxt);

//            synchronized (serverHandler.responseBody)
//            {
                setResponseBody(msgTxt);
//                serverHandler.setResponseBody(msgTxt);
//            }
            consumer.getConsumer().close();
            consumer.getSession().close();
            consumer.getConn().close();
            return msgTxt;
//            c.getConsumer().setMessageListener(new MessageListener() {
//                @Override
//                public void onMessage(Message message) {
//                    try {
//                        String msgTxt = ((TextMessage) message).getText();
////                        JSONObject responseMap = new JSONObject(msgTxt);
////                        Jedis cache = null;
////                        switch (responseMap.getString("app"))
////                        {
////                            case "user": cache = Cache.userCache;
////                                break;
////                            case "tweet": cache = Cache.tweetCache;
////                                break;
////                            case "list": cache = ListCache.listCache;
////                                break;
////                            case "dm": cache = Cache.dmCache;
////                                break;
////                        }
////                        String method = responseMap.getString("method");
////                        if (!cache.exists(method)
////                                && (method.startsWith("get") || method.equals("user_tweets")
////                                || method.equals("timeline"))) {
////                            responseMap.put("cacheStatus", "valid");
////                            cache.set(responseMap.getString("method"), responseMap.toString());
////                        }
////                        msgTxt = responseMap.toString();
////                        sleep(1000);  //Why sleep?
//                        synchronized (serverHandler) {
//                            setResponseBody(msgTxt);
//                            serverHandler.setResponseBody(msgTxt);
//                            serverHandler.notifyAll();
//                            System.out.println("netty notified by notifier");
//                        }
////                        System.out.println("thread response " + getResponseBody());
//                    } catch (JMSException e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
////                    catch (JSONException e) {
////                        e.printStackTrace();
////                    }
//                }
//            });
        } catch (JMSException e) {
            // TODO Auto-generated catch block

            e.printStackTrace();
            return null;
        }
    }

    public EduMsgNettyServerHandler getServerHandler() {
        return serverHandler;
    }

    public void setServerHandler(EduMsgNettyServerHandler serverHandler) {
        this.serverHandler = serverHandler;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }
}
