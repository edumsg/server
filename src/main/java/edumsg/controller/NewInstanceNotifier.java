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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Callable;

import static edumsg.controller.MainServerMigration.getCounter;


public class NewInstanceNotifier implements Callable<String> {

    private EduMsgControllerHandler controllerHandler;
    private int app_num;

    public NewInstanceNotifier(int app_num, EduMsgControllerHandler controllerHandler) {
        this.app_num = app_num;
        this.controllerHandler = controllerHandler;
    }

    @Override
    public String call() {
        try {

            MainServerMigration newInstance = new MainServerMigration();
            newInstance.setUp(this.app_num, this.controllerHandler.getCorrelationId(), this.controllerHandler.log);
            File configFile = new File("IPs.properties");

            FileReader reader = new FileReader(configFile);
            Properties props = new Properties();
            props.load(reader);

            String newIp = props.getProperty("ip" + (getCounter() - 1));
            reader.close();
            return "{app:  \"server_" + (getCounter() - 1) + " \",msg: \"" + newIp + "\",code: \"200\",command:\"newInstance\"}";

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

