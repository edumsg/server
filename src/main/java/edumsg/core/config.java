package edumsg.core;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class config {
    public static int instance_num;
    public static String instance_user;
    public static String instance_host;
    public static String instance_pass;
    public static String main_host;

    // configuration file to get the info related to each micro-service before running it
    public static void initialize() throws Exception {
        String file = System.getProperty("user.dir") + "/config.conf";
        java.util.List<String> lines = new ArrayList<String>();
        Pattern pattern = Pattern.compile("\\[(.+)\\]");
        Matcher matcher;
        Stream<String> stream = Files.lines(Paths.get(file));
        lines = stream.filter(line -> !line.startsWith("#")).collect(Collectors.toList());

        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("instance_num")) {
                matcher = pattern.matcher(lines.get(i));
                if (matcher.find()) {
                    instance_num = Integer.parseInt(matcher.group(1));
                }
            }

            if (lines.get(i).contains("instance_user")) {
                matcher = pattern.matcher(lines.get(i));
                if (matcher.find()) {
                    instance_user = matcher.group(1);
                }
            }
            if (lines.get(i).contains("instance_host")) {
                matcher = pattern.matcher(lines.get(i));
                if (matcher.find()) {
                    instance_host = matcher.group(1);
                }
            }
            if (lines.get(i).contains("instance_pass")) {
                matcher = pattern.matcher(lines.get(i));
                if (matcher.find()) {
                    instance_pass = matcher.group(1);
                }
            }
            if (lines.get(i).contains("main_host")) {
                matcher = pattern.matcher(lines.get(i));
                if (matcher.find()) {
                    main_host = matcher.group(1);
                }
            }
        }
    }

    public static int getInstance_num() throws Exception {
        initialize();
        return instance_num;
    }

    public static String getInstance_user() throws Exception {
        initialize();
        return instance_user;
    }

    public static String getInstance_host() throws Exception {
        initialize();
        return instance_host;
    }

    public static String getInstance_pass() {
        return instance_pass;
    }

    public static String getMain_host() throws Exception {
        initialize();
        return main_host;
    }
}
