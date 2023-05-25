package edumsg.loadBalancer.admin;

import asg.cliche.Command;
import asg.cliche.Shell;
import asg.cliche.ShellFactory;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AdminShell {
    public static void main(String[] args) throws Exception {
        Shell shell = ShellFactory.createConsoleShell("AdminCLI", "Admin CLI", new AdminShell());
        shell.commandLoop();
    }

    public static String[] sendToController(JSONObject body) {
        try {
            URL url = new URL("http://localhost:9090/");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            String data = body.toString();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Content-Length", Integer.toString(data.length()));
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(data);
            outputStream.flush();
            outputStream.close();
            int responseCode = connection.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            connection.disconnect();
            JSONObject reposnseJson = new JSONObject(response.toString());
            return new String[]{reposnseJson.getString("msg").trim(), responseCode + ""};
        } catch (IOException e) {
            return new String[]{"Server error", "400"};
        }
    }

    public static String[] sendToLoadBalancer(JSONObject body) {
        try {
            URL url = new URL("http://localhost:7070/");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            String data = body.toString();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Content-Length", Integer.toString(data.length()));
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(data);
            outputStream.flush();
            outputStream.close();
            int responseCode = connection.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            connection.disconnect();
            JSONObject reposnseJson = new JSONObject(response.toString());
            return new String[]{reposnseJson.getString("msg").trim(), responseCode + ""};
        } catch (IOException e) {
            return new String[]{"Server error", "400"};
        }
    }

    @Command(description = "Set the maximum number of threads for a specific app")
    public String maxthreads(String app, int appNum, String maxThreads) {
        String response = sendToController(createJson(app, appNum, maxThreads))[0];
        return response;
    }

    @Command(description = "Set the maximum number of DB connections for a specific app")
    public String maxdb(String app, int appNum, String maxDBConn) {
        String response = sendToController(createJson(app, appNum, maxDBConn))[0];
        return response;
    }

    @Command(description = "Set the initial number of DB connections for a specific app")
    public String initdb(String app, int appNum, String initialDBConn) {
        String response = sendToController(createJson(app, appNum, initialDBConn))[0];
        return response;
    }

    @Command(description = "Set the log path for a specific app")
    public String logpath(String app, int appNum, String logPath) {
        String response = sendToController(createJson(app, appNum, logPath))[0];
        return response;
    }

    @Command(description = "starts a specific app")
    public String start(String app, int appNum) {
        String[] response = sendToController(createJson(app, appNum, "0"));
        if (response[1].equals("200")) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("command", "start");
            jsonObject.put("config", "true");
            jsonObject.put("app", app.toLowerCase() + "_" + appNum);
            String[] lbResponse = sendToLoadBalancer(jsonObject);
            if (lbResponse[1].equals("400"))
                return lbResponse[0];
        }
        return response[0];
    }

    @Command(description = "stops a specific app")
    public String stop(String app, int appNum) {
        String[] response = sendToController(createJson(app, appNum, "0"));
        if (response[1].equals("200")) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("command", "stop");
            jsonObject.put("config", "true");
            jsonObject.put("app", app.toLowerCase() + "_" + appNum);
            String[] lbResponse = sendToLoadBalancer(jsonObject);
            if (lbResponse[1].equals("400"))
                return lbResponse[0];
        }
        return response[0];
    }

    @Command(description = "shutdowns a specific app")
    public String shutdown(String app, int appNum) {
        String response = sendToController(createJson(app, appNum, "0"))[0];
        return response;
    }

    public JSONObject createJson(String app, int appNum, String parameters) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("command", "start");
        jsonObject.put("app_type", app.toLowerCase());
        jsonObject.put("app_num", appNum);
        jsonObject.put("parameters", parameters);
        return jsonObject;
    }
}
