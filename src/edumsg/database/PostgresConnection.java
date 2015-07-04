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

package edumsg.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.dbcp2.PoolingDriver;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class PostgresConnection {
	private static final Logger LOGGER = Logger
			.getLogger(PostgresConnection.class.getName());
	private static final String DB_USERNAME = "";   //your db username
	private static final String DB_PASSWORD = ""; //your db password
	private static final String DB_NAME = "EduMsgReplicaTest";
	private static final String DB_INIT_CONNECTIONS = "10";
	private static final String DB_MAX_CONNECTIONS = "15";
	private static final String URI = "jdbc:postgresql://localhost:5432/"
			+ DB_NAME;
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

			Properties props = new Properties();
			props.setProperty("user", DB_USERNAME);
			props.setProperty("password", DB_PASSWORD);
			props.setProperty("initialSize", DB_INIT_CONNECTIONS);
			props.setProperty("maxActive", DB_MAX_CONNECTIONS);

			ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(
					URI, props);
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
}
