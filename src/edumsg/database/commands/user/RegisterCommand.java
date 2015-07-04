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

package edumsg.database.commands.user;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.json.JSONException;
import org.json.JSONObject;
import org.postgresql.util.PSQLException;

import edumsg.database.BCrypt;
import edumsg.database.Command;
import edumsg.database.CommandsHelp;
import edumsg.database.PostgresConnection;
import edumsg.redis.EduMsgRedis;
import edumsg.shared.MyObjectMapper;

public class RegisterCommand implements Command, Runnable {
	private final Logger LOGGER = Logger.getLogger(RegisterCommand.class
			.getName());
	private HashMap<String, String> map;

	@Override
	public void setMap(HashMap<String, String> map) {
		this.map = map;
	}

	@Override
	public void execute() {
		Connection dbConn = null;
		CallableStatement proc = null;
		try {
			dbConn = PostgresConnection.getDataSource().getConnection();
			dbConn.setAutoCommit(true);
			String password = BCrypt.hashpw(map.get("password"), BCrypt.gensalt());
			if (map.containsKey("avatar_url")) {
				proc = dbConn
						.prepareCall("{call create_user(?,?,?,?,now()::timestamp,?)}");

			} else {
				proc = dbConn
						.prepareCall("{call create_user(?,?,?,?,now()::timestamp)}");
			}

			proc.setPoolable(true);
			proc.setString(1, map.get("username"));
			proc.setString(2, map.get("email"));
			proc.setString(3, password);
			proc.setString(4, map.get("name"));

			if (map.containsKey("avatar_url")) {
				proc.setString(5, map.get("avatar_url"));
			}

			proc.execute();

			MyObjectMapper mapper = new MyObjectMapper();
			JsonNodeFactory nf = JsonNodeFactory.instance;
			ObjectNode root = nf.objectNode();
			root.put("app", map.get("app"));
			root.put("method", map.get("method"));
			root.put("status", "ok");
			root.put("code", "200");
			try {
				CommandsHelp.submit(map.get("app"),
						mapper.writeValueAsString(root),
						map.get("correlation_id"), LOGGER);
				String cacheEntry = EduMsgRedis.redisCache.get("get_users");
				if(cacheEntry != null){
					JSONObject cacheEntryJson = new JSONObject(cacheEntry);
					cacheEntryJson.put("cacheStatus", "invalid");
					EduMsgRedis.redisCache.set("get_users", cacheEntryJson.toString());
				}
			} catch (JsonGenerationException e) {
				//Logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (JsonMappingException e) {
				//Logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (IOException e) {
				//Logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (PSQLException e) {
			if (e.getMessage().contains("unique constraint")) {
				if (e.getMessage().contains("(username)")) {
					CommandsHelp.handleError(map.get("app"), map.get("method"),
							"Username already exists",
							map.get("correlation_id"), LOGGER);
				}
				if (e.getMessage().contains("(email)")) {
					CommandsHelp.handleError(map.get("app"), map.get("method"),
							"Email already exists", map.get("correlation_id"),
							LOGGER);
				}
			} else {
				CommandsHelp.handleError(map.get("app"), map.get("method"),
						e.getMessage(), map.get("correlation_id"), LOGGER);
			}

			//Logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (SQLException e) {
			CommandsHelp.handleError(map.get("app"), map.get("method"),
					e.getMessage(), map.get("correlation_id"), LOGGER);
			//Logger.log(Level.SEVERE, e.getMessage(), e);
		} finally {
			PostgresConnection.disconnect(null, proc, dbConn);
		}
	}

	@Override
	public void run() {
		execute();
	}
}
