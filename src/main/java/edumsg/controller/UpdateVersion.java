package edumsg.controller;


import com.jcraft.jsch.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class UpdateVersion {

    // the controller actions to update class version
    public static void Update(String msg, String correlationId, Logger log) throws IOException {

        JSONObject requestJson = new JSONObject(msg);
        String app_type = requestJson.getString("app_type");
        String class_path = requestJson.getString("url");
        String class_name = requestJson.getString("class_name");
        String servers_list = requestJson.getString("servers_list");
        try {
            // for all the instances of a specific micro-service we send a zipped file containing the updated version
            // looping over the servers list, we will send the file to these nodes one by one
            JSONArray jsonArray = new JSONArray(servers_list);
            for (int i = 0; i < jsonArray.length(); i++) {
                String user = jsonArray.getJSONObject(i).getString("user");
                String password = jsonArray.getJSONObject(i).getString("password");
                String ip = jsonArray.getJSONObject(i).getString("ip");

                if (ip.contains("localhost")) {
                    Files.copy(Paths.get(class_path + ".java"),
                            Paths.get("C:\\Users\\OS\\Desktop\\bach\\server-master\\src\\main\\java\\edumsg\\core\\commands\\" + app_type.toLowerCase() + "\\" + class_name + ".java"), REPLACE_EXISTING);
                } else {

                    // compile the class then put it in a zip file then send it to the remote machine
                    compile(class_path);
                    zipping(class_path, class_name);
                    String remote_des = "/home/" + user + "/Desktop/";
                    SCPtoRemoteServer(user, password, ip, class_path, remote_des, app_type, class_name);
                }
            }
        } catch (Exception e) {
            // TODO: 23/04/2023 handle error 
            //controllerHandleError(app_type, 1, "updateClass", e.getMessage() + ": failed to update class version", correlationId, log);
        }

    }

    // method to compile .java file
    public static void compile(String class_path) {

        File sourceFile = new File(class_path + ".java");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        compiler.run(null, null, null, sourceFile.getPath());
    }

    //method to put .java file in zip file
    public static void zipping(String class_path, String class_name) throws IOException {
        // input file
        FileInputStream in = new FileInputStream(class_path + ".java");
        // out put file
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(class_path + ".zip"));
        // name the file inside the zip  file
        out.putNextEntry(new ZipEntry(class_name + ".java"));
        byte[] b = new byte[1024];
        int count;
        while ((count = in.read(b)) > 0) {
            out.write(b, 0, count);
        }
        out.close();
        in.close();
    }

    // connect to the remote server over SFTP channel and send zip file
    public static void SCPtoRemoteServer(String user, String password, String host, String class_path, String remote_des, String app_type, String class_name) throws JSchException, SftpException {
        int port = 22;
        JSch jsch = new JSch();
        Session session = jsch.getSession(user, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
        sftpChannel.connect();
        System.out.println("SFTP Channel created...");
        System.out.println(class_path);
        sftpChannel.put(class_path + ".zip", remote_des + app_type.toLowerCase()
                + "/src/main/java/edumsg/core/commands/" + app_type.toLowerCase() + "/" + class_name);
        sftpChannel.disconnect();
        session.disconnect();
    }

}

