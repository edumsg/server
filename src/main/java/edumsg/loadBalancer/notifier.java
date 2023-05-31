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

import io.netty.util.CharsetUtil;
import org.json.JSONObject;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;


public class notifier implements Callable<String> {


    public CountDownLatch latch;
    private loadBalancerServerHandler loadBalancer;
    private String response;


    public notifier(loadBalancerServerHandler balancer) {
        this.loadBalancer = balancer;
    }

    @Override
    public String call() throws Exception {
        // create latch with count 1 to await the call method until the response is sent back from the main server
        latch = new CountDownLatch((1));
        JSONObject body = new JSONObject(loadBalancer.getByteBuf().toString(CharsetUtil.UTF_8));
        if (body.has("config")) {
            switch (body.getString("command")) {
                case "newInstance":
                    Calculation.new_instance(body.getString("app").split("_")[0], body.getString("ip"));
                    break;
                case "start":
                    Calculation.reflect_command(body.getString("app"), true);
                    break;
                case "stop":
                    Calculation.reflect_command(body.getString("app"), false);
                    break;
            }
            JSONObject response = new JSONObject();
            response.put("msg", "success");
            response.put("code", "200");
            return response.toString();
        }
        HttpSnoopClientHandler.add_notifier(loadBalancer.getId(), this);
        HttpSnoopClient.serverCluster(loadBalancer.getByteBuf(), loadBalancer.getId());
        latch.await();
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public CountDownLatch getLatch() {
        return latch;
    }
}

