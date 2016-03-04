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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.POJONode;
import org.postgresql.util.PSQLException;

import edumsg.core.Command;
import edumsg.core.CommandsHelp;
import edumsg.core.Conversation;
import edumsg.core.DirectMessage;
import edumsg.core.PostgresConnection;
import edumsg.core.User;
import edumsg.shared.MyObjectMapper;

public class GetConversationsCommand implements Command, Runnable {
	private final Logger LOGGER = Logger
			.getLogger(GetConversationsCommand.class.getName());
	private HashMap<String, String> map;

	@Override
	public void setMap(HashMap<String, String> map) {
		this.map = map;
	}

	@Override
	public void execute() {
		Connection dbConn = null;
		CallableStatement proc = null;
		ResultSet set = null;
		try {
			dbConn = PostgresConnection.getDataSource().getConnection();
			dbConn.setAutoCommit(false);
			proc = dbConn.prepareCall("{? = call get_conversations(?)}");
			proc.setPoolable(true);
			proc.registerOutParameter(1, Types.OTHER);
			proc.setInt(2, Integer.parseInt(map.get("user_id")));
			proc.execute();

			set = (ResultSet) proc.getObject(1);

			MyObjectMapper mapper = new MyObjectMapper();
			JsonNodeFactory nf = JsonNodeFactory.instance;
			ObjectNode root = nf.objectNode();
			root.put("app", map.get("app"));
			root.put("method", map.get("method"));
			root.put("status", "ok");
			root.put("code", "200");

			ArrayList<Conversation> convs = new ArrayList<>();
			while (set.next()) {
				int conv_id = set.getInt(1);
				int sender_id = set.getInt(2);
				String sender_name = set.getString(3);
				int reciever_id = set.getInt(4);
				String reciever_name = set.getString(5);
				String dm_text = set.getString(6);
				Timestamp created_at = set.getTimestamp(7);

				User sender = new User();
				sender.setId(sender_id);
				sender.setName(sender_name);

				User reciever = new User();
				reciever.setId(reciever_id);
				reciever.setName(reciever_name);

				Conversation conv = new Conversation();
				conv.setId(conv_id);
				DirectMessage dm = new DirectMessage();
				dm.setSender(sender);
				dm.setReciever(reciever);
				dm.setDmText(dm_text);
				dm.setCreatedAt(created_at);
				conv.setLastDM(dm);

				convs.add(conv);
			}

			POJONode child = nf.POJONode(convs);
			root.put("convs", child);
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

			dbConn.commit();
		} catch (PSQLException e) {
			CommandsHelp.handleError(map.get("app"), map.get("method"),
					e.getMessage(), map.get("correlation_id"), LOGGER);
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		} catch (SQLException e) {
			CommandsHelp.handleError(map.get("app"), map.get("method"),
					e.getMessage(), map.get("correlation_id"), LOGGER);
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		} finally {
			PostgresConnection.disconnect(set, proc, dbConn);
		}
	}

	@Override
	public void run() {
		execute();
	}
}
