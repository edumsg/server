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

package edumsg.core;

import org.apache.commons.dbcp2.*;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PostgresConnection {
    private static final Logger LOGGER = Logger
            .getLogger(PostgresConnection.class.getName());
    private static String DB_USERNAME;   //your db username
    private static String DB_PASSWORD; //your db password
    private static String DB_PORT;
    private static String DB_HOST;
    private static String DB_NAME;
    private static final String DB_INIT_CONNECTIONS = "10";
    private static final String DB_MAX_CONNECTIONS = "15";
    private static String DB_URL;
    private static PoolingDriver dbDriver;
    private static PoolingDataSource<PoolableConnection> dataSource;

    public static void shutdownDriver() throws SQLException {
        dbDriver.closePool(DB_NAME);
    }

    public static void printDriverStats() throws SQLException {
        ObjectPool<? extends Connection> connectionPool = dbDriver
                .getConnectionPool(DB_NAME);

        System.out.println("DB Active Connections: "
                + connectionPool.getNumActive());
        System.out.println("DB Idle Connections: "
                + connectionPool.getNumIdle());
    }

    public static PoolingDataSource<PoolableConnection> getDataSource() {
        return dataSource;
    }

    public static void disconnect(ResultSet rs, PreparedStatement statment,
                                  Connection conn, Statement query) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
            }
        }
        if (statment != null) {
            try {
                statment.close();
            } catch (SQLException e) {
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
            }
        }

        if(query != null){
            try {
                query.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void disconnect(ResultSet rs, PreparedStatement statment,
                                  Connection conn) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
            }
        }
        if (statment != null) {
            try {
                statment.close();
            } catch (SQLException e) {
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
            }
        }


    }

    public static void initSource() {
        try {
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException ex) {
                LOGGER.log(Level.SEVERE,
                        "Error loading Postgres driver: " + ex.getMessage(), ex);
            }
            try{
                readConfFile();
            }   catch (Exception e){
                e.printStackTrace();
            }
            Properties props = new Properties();
          //  System.out.println(DB_USERNAME);
            props.setProperty("user", DB_USERNAME);
            props.setProperty("password", DB_PASSWORD);
            props.setProperty("initialSize", DB_INIT_CONNECTIONS);
            props.setProperty("maxActive", DB_MAX_CONNECTIONS);

            ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(
                    DB_URL, props);
            PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(
                    connectionFactory, null);
            poolableConnectionFactory.setPoolStatements(true);

            GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
            poolConfig.setMaxIdle(Integer.parseInt(DB_INIT_CONNECTIONS));
            poolConfig.setMaxTotal(Integer.parseInt(DB_MAX_CONNECTIONS));
            ObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(
                    poolableConnectionFactory, poolConfig);
            poolableConnectionFactory.setPool(connectionPool);

            Class.forName("org.apache.commons.dbcp2.PoolingDriver");
            dbDriver = (PoolingDriver) DriverManager
                    .getDriver("jdbc:apache:commons:dbcp:");
            dbDriver.registerPool(DB_NAME, connectionPool);

            dataSource = new PoolingDataSource<>(connectionPool);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Got error initializing data source: "
                    + ex.getMessage(), ex);
        }
    }

    public static void setDBUser(String name) {
        DB_USERNAME = name;
    }

    public static void setDBPassword(String pass) {
        DB_PASSWORD = pass;
    }

    public static void setDBPort(String port) {
        DB_PORT = port;
    }

    public static void setDBHost(String host) {
        DB_HOST = host;
    }

    public static void setDBURL(String url) {
        DB_URL = url;
    }

    public static void setDBName(String name) {
        DB_NAME = name;
    }

    private static boolean formatURL() {
        setDBURL("jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME);
        System.out.println(DB_URL);
        Pattern pattern = Pattern.compile("^\\w+:\\w+:\\/{2}\\w+:\\d+\\/\\w+(?:\\W|\\w)*$");
        Matcher matcher = pattern.matcher(DB_URL);
        return matcher.matches();
    }

    public static void readConfFile() throws Exception {
        String file = System.getProperty("user.dir") + "/Postgres.conf";
        java.util.List<String> lines = new ArrayList<String>();
        //extract string between square brackets and compile regex for faster performance
        Pattern pattern = Pattern.compile("\\[(.+)\\]");
        Matcher matcher;
        Exception e;
        Stream<String> stream = Files.lines(Paths.get(file));
        lines = stream.filter(line -> !line.startsWith("#")).collect(Collectors.toList());

        //set variables based on matches
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("user")) {
                matcher = pattern.matcher(lines.get(i));
                if(matcher.find())
                    setDBUser(matcher.group(1));
                 else
                    throw  e = new Exception("empty user in Postgres.conf");
            }
            if (lines.get(i).contains("database")) {
                matcher = pattern.matcher(lines.get(i));
                if(matcher.find())
                    setDBName(matcher.group(1));
                else
                    throw  e = new Exception("empty database name in Postgres.conf");
            }
            if (lines.get(i).contains("pass")) {
                matcher = pattern.matcher(lines.get(i));
                matcher.find();
                setDBPassword(matcher.group(1));
            }
            if (lines.get(i).contains("host")) {
                matcher = pattern.matcher(lines.get(i));
                if (matcher.find())
                    setDBHost(matcher.group(1));
                else
                    setDBHost("localhost");
            }
            if (lines.get(i).contains("port")) {
                matcher = pattern.matcher(lines.get(i));
                if (matcher.find())
                    setDBPort(matcher.group(1));
                else
                    setDBPort("5432");
            }
        }
        if (!formatURL()) {
             e = new Exception("Wrong Format in Postgres.conf");
            throw e;
        }
    }

}
