package edumsg.logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class MyLogger {
    private String log_path;
    private Logger MyLogger;
    public void initialize(Logger logger , String path) {
        log_path = path;
        MyLogger = logger;
        try {
            LogManager.getLogManager().readConfiguration(new FileInputStream("src/main/java/edumsg/logger/logger.properties"));
            logger.setLevel(Level.INFO);
            File file = new File(path+"\\"+logger.getName());
            file.mkdir();
            Handler fileHandler = new FileHandler(path+"\\"+logger.getName()+"\\logger.log", 100000, 1);
            logger.addHandler(fileHandler);
        } catch (SecurityException | IOException e1) {
            e1.printStackTrace();
        }
    }

    public void setLog_path(String log_path) {
        this.log_path = log_path;
        initialize(MyLogger,log_path);
    }

    public String getLog_path() {
        return log_path;
    }
}

