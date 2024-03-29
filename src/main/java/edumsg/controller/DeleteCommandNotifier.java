package edumsg.controller;

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

import edumsg.activemq.ActiveMQConfig;
import edumsg.activemq.publisher;
import org.json.JSONObject;

import java.util.concurrent.Callable;
import java.util.logging.Logger;


public class DeleteCommandNotifier implements Callable<String> {

    private JSONObject request;
    private String correlationId;
    private Logger log;

    private String app_type;

    public DeleteCommandNotifier(String app_type, JSONObject request, String correlationId, Logger log) {
        this.request = request;
        this.correlationId = correlationId;
        this.log = log;
        this.app_type = app_type.toLowerCase();
    }

    @Override
    public String call() {


        try {
            publisher topic = new publisher(new ActiveMQConfig(app_type.toUpperCase()));
            topic.publish(request.toString());
            String msg = "{Command deleted successfully}";
            return "{app:  \"" + app_type + " \",msg: \"" + msg + "\",code: \"200\",command:\"AddCommand\"}";
        } catch (Exception ex) {
            ex.printStackTrace();
            String msg = "{Error}";
            return "{app:  \"" + app_type + " \",msg: \"" + msg + "\",code: \"400\",command:\"AddCommand\"}";
        }
    }

}

