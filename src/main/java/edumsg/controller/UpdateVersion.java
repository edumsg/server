package edumsg.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static edumsg.shared.controllerResponse.controllerHandleError;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class UpdateVersion {

    // the controller actions to update class version
    public static void Update(String msg) throws IOException {
        JSONObject requestJson = new JSONObject(msg);
        String app_type = requestJson.getString("app_type");
        String class_path = requestJson.getString("url");
        String class_name = requestJson.getString("class_name");
        String servers_list = requestJson.getString("servers_list");

        // for each instance of a specific app we send file zipped file containing the updated version
        // looping over the servers list
        JSONArray jsonArray = new JSONArray(servers_list);
        for(int i = 0 ; i < jsonArray.length() ; i++) {
            String user = jsonArray.getJSONObject(i).getString("user");
            String password = jsonArray.getJSONObject(i).getString("password");
            String ip = jsonArray.getJSONObject(i).getString("ip");

            if (ip.contains("localhost")) {
                Files.copy(Paths.get(class_path+".java"),
                        Paths.get("C:\\Users\\OS\\Desktop\\bach\\server-master\\src\\main\\java\\edumsg\\core\\commands\\"+app_type.toLowerCase()+"\\"+class_name+".java"), REPLACE_EXISTING);
            } else {

                // compile the class then put it in a zip file
                compile(class_path,app_type);
                zipping(class_path, class_name);
                String remote_des = "/home/"+user+"Desktop/";
                SCPtoRemoteServer(user, password, ip, class_path, remote_des,app_type);
            }
        }

    }

    // method to compile .java file
    public static void compile(String class_path , String app_type) throws JsonProcessingException {
        try {
            File sourceFile = new File(class_path);
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            compiler.run(null, null, null, sourceFile.getPath());
        }catch (Exception e){
            controllerHandleError(app_type,1,"updateClass",e.getMessage(),null,null);
        }
    }
    //method to put .java file in zip file
    public static void zipping (String path , String class_name) throws IOException {

        // input file
        FileInputStream in = new FileInputStream(path);
        // out put file
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(path + ".zip"));
        // name the file inside the zip  file
        out.putNextEntry(new ZipEntry(class_name));
        byte[] b = new byte[1024];
        int count;
        while ((count = in.read(b)) > 0) {
            out.write(b, 0, count);
        }
        out.close();
        in.close();
    }
    // connect to remote server over SFTP channel and send zip file
    public static void SCPtoRemoteServer (String user , String password , String host,String class_path , String remote_des,String app_type) throws JsonProcessingException {
        int port= 22;
        try
        {
                JSch jsch = new JSch();
                Session session = jsch.getSession(user, host, port);
                session.setPassword(password);
                session.setConfig("StrictHostKeyChecking", "no");
                session.connect();
                ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
                sftpChannel.connect();
                System.out.println("SFTP Channel created...");
                sftpChannel.put(class_path + ".zip", remote_des);
                sftpChannel.disconnect();
                session.disconnect();
        }
        catch(JSchException | SftpException e) {
            controllerHandleError(app_type,1,"updateClass",e.getMessage(),null,null);

        }
    }

}
