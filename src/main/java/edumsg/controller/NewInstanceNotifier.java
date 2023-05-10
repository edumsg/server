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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Logger;


public class NewInstanceNotifier implements Callable<String> {

    private int app_num;
    private String correlationId;
    private Logger log;

    private String app_type;

    public NewInstanceNotifier(String app_type, int app_num, String correlationId, Logger log) {
        this.app_num = app_num;
        this.correlationId = correlationId;
        this.log = log;
        this.app_type = app_type.toLowerCase();
    }

    @Override
    public String call() {
        try {

            MainServerMigration mainServerMigration = new MainServerMigration();
            mainServerMigration.setUp(app_type, app_num, correlationId, log);
            String msg = "{App deployed successfully}";
            String ip = mainServerMigration.getIp();
            return "{app:  \"" + app_type + "_" + app_num + " \",msg: \"" + msg + "\",code: \"200\",command:\"newInstance\",ip:\"" + ip + "\"}";

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

