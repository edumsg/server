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

package edumsg.core.commands.dm;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.postgresql.util.PSQLException;

import edumsg.core.Command;
import edumsg.core.CommandsHelp;
import edumsg.core.PostgresConnection;
import edumsg.shared.MyObjectMapper;

public class CreateDmCommand implements Command, Runnable {
	private final Logger LOGGER = Logger.getLogger(CreateDmCommand.class
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
			if (map.containsKey("image_url")) {
				proc = dbConn
						.prepareCall("{? = call create_dm(?,?,?,now()::timestamp,?))}");

			} else {
				proc = dbConn
						.prepareCall("{? = call create_dm(?,?,?,now()::timestamp)}");
			}

			proc.setPoolable(true);
			proc.registerOutParameter(1, Types.BOOLEAN);
			proc.setInt(2, Integer.parseInt(map.get("sender_id")));
			proc.setInt(3, Integer.parseInt(map.get("reciever_id")));
			proc.setString(4, map.get("dm_text"));
			if (map.containsKey("image_url")) {
				proc.setString(5, map.get("image_url"));
			}
			proc.execute();

			boolean sent = proc.getBoolean(1);

			if (sent) {
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
				} catch (JsonGenerationException e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				} catch (JsonMappingException e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				} catch (IOException e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
			} else {
				CommandsHelp.handleError(map.get("app"), map.get("method"),
						"You can not dm a user who is not following you",
						map.get("correlation_id"), LOGGER);
			}

		} catch (PSQLException e) {
			if (e.getMessage().contains("value too long")) {
				CommandsHelp.handleError(map.get("app"), map.get("method"),
						"DM length cannot exceed 140 character",
						map.get("correlation_id"), LOGGER);
			} else {
				CommandsHelp.handleError(map.get("app"), map.get("method"),
						e.getMessage(), map.get("correlation_id"), LOGGER);
			}

			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		} catch (SQLException e) {
			CommandsHelp.handleError(map.get("app"), map.get("method"),
					e.getMessage(), map.get("correlation_id"), LOGGER);
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		} finally {
			PostgresConnection.disconnect(null, proc, dbConn);
		}
	}

	@Override
	public void run() {
		execute();
	}
}
