package edumsg.logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.*;

public class MyLogger {
    private String log_path;
    private Logger MyLogger;
    // initialize the configurations to create a logger for a micro-service
    public void initialize(Logger logger , String path) {
        log_path = path;
        MyLogger = logger;
        try {
            // set the logging properties from a config file
            LogManager.getLogManager().readConfiguration(new FileInputStream("src/main/java/edumsg/logger/logger.properties"));
            logger.setLevel(Level.INFO);
            // create the directory when we will record the logs information
            File file = new File(path+"\\"+logger.getName());
            file.mkdir();
            Handler fileHandler = new FileHandler(path+"\\"+logger.getName()+"\\logger.log", 100000, 1);
            logger.addHandler(fileHandler);
        } catch (SecurityException | IOException e1) {
            e1.printStackTrace();
        }
    }
    // change the directory against set_log_path command
    public void setLog_path(String log_path) {
        this.log_path = log_path;
        initialize(MyLogger,log_path);
    }

    public String getLog_path() {
        return log_path;
    }
}

