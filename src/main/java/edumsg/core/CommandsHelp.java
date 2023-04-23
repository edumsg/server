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

package edumsg.core;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edumsg.activemq.ActiveMQConfig;
import edumsg.activemq.Producer;
import edumsg.shared.MyObjectMapper;

import java.io.IOException;
import java.util.logging.Logger;

public class CommandsHelp {

    private static int cur_instance;

    static {
        try {
            cur_instance = config.getInstance_num();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void handleError(String app, String method, String errorMsg,
                                   String correlationID, Logger logger) {
        JsonNodeFactory nf = JsonNodeFactory.instance;
        MyObjectMapper mapper = new MyObjectMapper();
        ObjectNode node = nf.objectNode();
        node.put("app", app);
        node.put("method", method);
        node.put("status", "Bad Request");
        node.put("code", "400");
        node.put("message", errorMsg);
        try {
            submit(app, mapper.writeValueAsString(node), correlationID, logger);
        } catch (IOException e) {
            //logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }


    public static void submit(String app, String json, String correlationID,
                              Logger logger) {

        Producer p = new Producer(new ActiveMQConfig(app.toUpperCase()
                + "_" + cur_instance + ".OUTQUEUE"));
        p.send(json, correlationID, logger);
    }
}

