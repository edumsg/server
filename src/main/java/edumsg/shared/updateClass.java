package edumsg.shared;

import com.fasterxml.jackson.core.JsonProcessingException;
import edumsg.core.CommandsMap;
import edumsg.core.config;
import org.json.JSONObject;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static edumsg.shared.controllerResponse.controllerHandleError;


public class updateClass {
    private static int instance_num;
    private static String instance_user;
    private static String instance_host;

    public updateClass() throws Exception {
    }

    public static void init (String msg) throws Exception {
        JSONObject requestJson = new JSONObject(msg);
        String app_type = requestJson.getString("app_type");
        String class_name = requestJson.getString("class_name");
        instance_num = config.getInstance_num();
        instance_user = config.getInstance_user();
        instance_host = config.getInstance_host();
        String path;
        if(instance_host.equals("localhost")){
            path = "C:\\Users\\OS\\Desktop\\bach\\server-master\\src\\main\\java\\edumsg\\core\\commands\\"+app_type.toLowerCase()+"\\"+ class_name;
        }
        else{
            path ="home/"+instance_user+"/Desktop/"+app_type+"/src/main/java/edumsg/core/commands/"+app_type.toLowerCase()+"/"+ class_name;
            unzip("home/"+instance_user+"/Desktop",class_name,app_type);
        }
        compile(path,class_name,app_type);
        loadClass(app_type,class_name,path);
    }

    public static void unzip(String remote_des , String class_name,String app_type) throws IOException {

try {
    File destDir = new File(remote_des + "/" + app_type + "/src/main/java/edumsg/core/commands/" + app_type.toLowerCase() + "/" + class_name + ".java");

    byte[] buffer = new byte[1024];
    ZipInputStream zis = new ZipInputStream(new FileInputStream(remote_des + class_name + ".zip"));
    ZipEntry zipEntry = zis.getNextEntry();
    while (zipEntry != null) {
        File newFile = newFile(destDir, zipEntry);
        FileOutputStream fos = new FileOutputStream(newFile);
        int len;
        while ((len = zis.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
        }
        fos.close();
        zipEntry = zis.getNextEntry();
    }

    zis.closeEntry();
    zis.close();
}catch (Exception e){
    controllerHandleError(app_type,1,"updateClass",e.getMessage(),null,null);
}
    }

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;

    }
    public static void compile(String path, String class_name,String app_type) throws JsonProcessingException {
        try {
            File sourceFile = new File(path+".java");
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            compiler.run(null, null, null, sourceFile.getPath());
            System.out.println("compiled...");
        }catch (Exception e){
            controllerHandleError(app_type,1,"updateClass",e.getMessage(),null,null);
        }
    }
    public static void loadClass (String app_type , String class_name,String path) throws JsonProcessingException, InterruptedException {
        try {
            loader load = new loader();
            Class<?> cls = load.findClass("edumsg.core.commands." + app_type.toLowerCase() + "." + class_name, path);
            String command_key = CommandsMap.map("class edumsg.core.commands." + app_type.toLowerCase() + "." + class_name);
            CommandsMap.replace(command_key, cls);
            String response = "the class version updated successfully";
            if (instance_num == 1) {
                controllerResponse.controllerSubmit(app_type, instance_num, response, "updateClass", null, null);
            }
        }catch (Exception e){
            controllerHandleError(app_type,1,"updateClass",e.getMessage(),null,null);
        }
    }
}