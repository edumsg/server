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

import edumsg.core.Command;
import edumsg.core.CommandsHelp;
import edumsg.core.PostgresConnection;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.postgresql.util.PSQLException;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CreateDmCommand extends Command implements Runnable {
    private final Logger LOGGER = Logger.getLogger(CreateDmCommand.class
            .getName());


    @Override
    public void execute() {

        try {
            dbConn = PostgresConnection.getDataSource().getConnection();
            dbConn.setAutoCommit(true);
            Statement query = dbConn.createStatement();
            query.setPoolable(true);


            if (map.containsKey("image_url")) {
                set = query.executeQuery(String.format("SELECT * FROM create_dm(%s,%s,'%s',now()::timestamp," +
                        "'%s')",map.get("sender_id"),map.get("reciever_id"),map.get("dm_text"),map.get("image_url")));
            } else {
                set = query.executeQuery(String.format("SELECT * FROM create_dm(%s,%s,'%s',now()::timestamp)",map.get("sender_id"),map.get("reciever_id"),map.get("dm_text")));
            }

            boolean sent = proc.getBoolean(1);
            if (sent) {
                root.put("app", map.get("app"));
                root.put("method", map.get("method"));
                root.put("status", "ok");
                root.put("code", "200");
                try {
                    CommandsHelp.submit(map.get("app"), mapper.writeValueAsString(root), map.get("correlation_id"), LOGGER);
                } catch (JsonGenerationException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                } catch (JsonMappingException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            } else {
                CommandsHelp.handleError(map.get("app"), map.get("method"), "You can not dm a user who is not following you", map.get("correlation_id"), LOGGER);
            }

        } catch (PSQLException e) {
            if (e.getMessage().contains("value too long")) {
                CommandsHelp.handleError(map.get("app"), map.get("method"), "DM length cannot exceed 140 character", map.get("correlation_id"), LOGGER);
            } else {
                CommandsHelp.handleError(map.get("app"), map.get("method"), e.getMessage(), map.get("correlation_id"), LOGGER);
            }

            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } catch (SQLException e) {
            CommandsHelp.handleError(map.get("app"), map.get("method"), e.getMessage(), map.get("correlation_id"), LOGGER);
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            PostgresConnection.disconnect(null, proc, dbConn);
        }
    }
}
