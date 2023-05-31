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
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.logging.Logger;


public class AddCommandNotifier implements Callable<String> {

    private JSONObject request;
    private String correlationId;
    private Logger log;

    private String app_type;

    public AddCommandNotifier(String app_type, JSONObject request, String correlationId, Logger log) {
        this.request = request;
        this.correlationId = correlationId;
        this.log = log;
        this.app_type = app_type.toLowerCase();
    }

    @Override
    public String call() {
        publisher topic = new publisher(new ActiveMQConfig(app_type.toUpperCase()));
        String className = request.getString("class_name");
        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new ClassClassPath(EduMsgControllerHandler.class));

        try {
            CtClass ctClass = pool.get(className);
            byte[] classBytes = ctClass.toBytecode();
            String byteString = java.util.Base64.getEncoder().encodeToString(classBytes);
            request.put("byteCode", byteString);
            topic.publish(request.toString());
            renewJar();
            String msg = "{Command added successfully}";
            return "{app:  \"" + app_type + " \",msg: \"" + msg + "\",code: \"200\",command:\"AddCommand\"}";
        } catch (Exception ex) {
            ex.printStackTrace();
            String msg = "{Error}";
            return "{app:  \"" + app_type + " \",msg: \"" + msg + "\",code: \"400\",command:\"AddCommand\"}";
        }
    }

    private void renewJar() throws UnknownHostException {
        String currentDirectory = System.getProperty("user.dir");
        String command = "cd " + currentDirectory + " ; mvn package";
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("powershell.exe", "-Command", command);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            System.out.println("Exit Code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        TreeSet<Host> hosts = EduMsgController.hosts;
        for (Host host : hosts) {
            if (!host.getIp().equals(InetAddress.getLocalHost().getHostAddress())) {
                host.setHasJar(false);
            }
        }
    }
}

