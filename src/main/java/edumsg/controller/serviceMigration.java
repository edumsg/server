package edumsg.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jcraft.jsch.*;
import edumsg.shared.MyObjectMapper;
import org.zeroturnaround.zip.ZipUtil;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;


public class serviceMigration {
    private String user;
    private String password;
    private String ip;
    private int port=22;
    private Session session;
    private String app;
    private int instance;
    private static int counter = 1;


public void setUp (String app_type , int cur_instance) throws JSchException, IOException {
    app = app_type;
    instance = cur_instance;
    server_props();
    JSch jsch = new JSch();
    System.out.println(ip);
    session = jsch.getSession(user, ip, port);
    session.setPassword(password);
    session.setConfig("StrictHostKeyChecking", "no");
    session.connect();
    System.out.println("Connection established.");
    String run_class;
    switch (app_type.toUpperCase()){
        case "USER": run_class = "UserMain"; break;
        case "DM": run_class = "DMMain"; break;
        case "TWEET": run_class = "TweetMain"; break;
        case "LIST": run_class = "ListMain"; break;
        case "SERVER": run_class = "EduMsgNettyServer"; break;
        default:
            throw new IllegalStateException("Unexpected value: " + app_type.toUpperCase());
    }
    zipping();
    scpToServer ();
    unzipCommand ();
    install();
    run(run_class);
    System.out.println("running...");
    session.disconnect();

}
public void zipping () throws IOException {
    // write configuration file containing the instance number for the new instance
    File file = new File("C:\\Users\\OS\\Desktop\\Edumsg-comp\\Micro-services\\"+app+"\\"+app+"\\src\\main\\java\\config.conf");
    List<String> lines = Arrays.asList("# config attributes", "instance_num = ["+instance+"]","instance_user = ["+user+"]","instance_host = ["+ip+"]","instance_pass = ["+password+"]");
    Files.write(Paths.get(file.getPath()), lines, StandardCharsets.UTF_8);

    ZipUtil.pack(new File("C:\\Users\\OS\\Desktop\\Edumsg-comp\\Micro-services\\"+app), new File("C:\\Users\\OS\\Desktop\\Edumsg-comp\\Micro-services\\"+app+".zip"));
    System.out.println("zipped successfully");

}
    public void scpToServer (){
        try
        {
            ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            sftpChannel.put("C:\\Users\\OS\\Desktop\\Edumsg-comp\\Micro-services\\"+app+".zip", "/home/"+user+"/Desktop");
            sftpChannel.disconnect();
            System.out.println("package sent...");

        }
        catch(JSchException | SftpException e)
        {
            System.out.println(e);
        }
    }

    public void unzipCommand (){
    String Command1="sudo apt-get install -y unzip";
    String Command2 = "cd Desktop";
    String Command3 = "unzip "+app+".zip";

    try
    {
        Channel channel =session.openChannel("exec");
        ((ChannelExec)channel).setCommand("sudo -S -p '' "+Command1 + "&&" + Command2+ "&&" + Command3);
        channel.setInputStream(null);
        ((ChannelExec)channel).setErrStream(System.err);
        ((ChannelExec)channel).setPty(true);
        InputStream in=channel.getInputStream();
        OutputStream out=channel.getOutputStream();
        channel.connect();
        out.write((password+"\n").getBytes());
        out.flush();
        byte[] tmp=new byte[1024];
        print(in,tmp,channel);
        channel.disconnect();
    }
    catch(JSchException | IOException e)
    {
        System.out.println(e);
    }
}
    public void install(){
        String Command1="sudo apt-get -y install openjdk-8-jdk";
        try
        {
            Channel channel =session.openChannel("exec");
            ((ChannelExec)channel).setCommand("sudo -S -p '' "+Command1);
            channel.setInputStream(null);
            ((ChannelExec)channel).setErrStream(System.err);
            ((ChannelExec)channel).setPty(true);
            InputStream in=channel.getInputStream();
            OutputStream out=channel.getOutputStream();
            channel.connect();
            out.write((password+"\n").getBytes());
            out.flush();
            byte[] tmp=new byte[1024];
            print(in,tmp,channel);
            channel.disconnect();
        }
        catch(JSchException | IOException e)
        {
            System.out.println(e);
        }
    }

    public void run (String run_class){
        String Command1="cd ~";
        String Command2="cd Desktop/"+app.toLowerCase()+"/src/main/java";
        String Command3 = "javac -cp .:./jars/* edumsg/*/*.java";
        String Command4;
        if(run_class.equals("EduMsgNettyServer")) {
             Command4 = "java -cp .:./jars/* edumsg/netty/" + run_class;
        }else{
             Command4 = "java -cp .:./jars/* edumsg/shared/"+run_class;
        }

        try
        {
            Channel channel =session.openChannel("exec");
            ((ChannelExec)channel).setCommand(Command1+ "&&" + Command2+ "&&" + Command3+ "&&" + Command4);
            channel.setInputStream(null);
            ((ChannelExec)channel).setErrStream(System.err);
            ((ChannelExec)channel).setPty(true);
            InputStream in=channel.getInputStream();
            OutputStream out=channel.getOutputStream();
            channel.connect();
            out.write((password+"\n").getBytes());
            out.flush();
            byte[] tmp=new byte[1024];
            print(in,tmp,channel);
            channel.disconnect();
        }
        catch(JSchException | IOException e)
        {
            System.out.println(e);
        }
    }
    public void server_props(){
        File configFile = new File("IPs.properties");
        try {
            FileReader reader = new FileReader(configFile);
            Properties props = new Properties();
            props.load(reader);

            ip = props.getProperty("ip"+getCounter());
            user = props.getProperty("user"+getCounter());
            password = props.getProperty("password"+getCounter());
            System.out.println("server_props..."+ip +""+user + "" + password);
            reader.close();
            setCounter(getCounter()+1);
        } catch (FileNotFoundException ex) {
            // file does not exist
        } catch (IOException ex) {
            // I/O error
        }
    }

    public static void print (InputStream in , byte[] tmp , Channel channel) throws IOException {
        while(true){
            while(in.available()>0){
                int i=in.read(tmp, 0, 1024);
                if(i<0)break;
                System.out.print(new String(tmp, 0, i));
            }
            if(channel.isClosed()){
                System.out.println("exit-status: "+channel.getExitStatus());
                break;
            }
            try{Thread.sleep(1000);}catch(Exception ignored){}
        }
    }
    public void close (){
    session.disconnect();
    }

    public static int getCounter() {
        return counter;
    }

    public static void setCounter(int counter) {
        serviceMigration.counter = counter;
    }
}

