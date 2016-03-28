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

package edumsg.core.commands.user;

import edumsg.core.Command;
import edumsg.core.CommandsHelp;
import edumsg.core.PostgresConnection;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.postgresql.util.PSQLException;

import java.io.IOException;
import java.sql.Array;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

public class UpdateUserCommand extends Command implements Runnable {
    private final Logger LOGGER = Logger.getLogger(UpdateUserCommand.class.getName());

    @Override
    public void execute() {

        String app = map.get("app");
        String method = map.get("method");
        String correlationID = map.get("correlation_id");
        try {
            dbConn = PostgresConnection.getDataSource().getConnection();
            dbConn.setAutoCommit(true);
            proc = dbConn.prepareCall("{call edit_user(?,?)}");
            proc.setPoolable(true);
            proc.setInt(1, Integer.parseInt(map.get("user_id")));

            map.remove("user_id");
            map.remove("app");
            map.remove("method");
            map.remove("correlation_id");
            map.remove("queue");
            Set<Entry<String, String>> set = map.entrySet();
            Iterator<Entry<String, String>> iterator = set.iterator();
            String[][] arraySet = new String[set.size()][2];
            int i = 0;

            while (iterator.hasNext()) {
                Entry<String, String> entry = iterator.next();
                String[] temp = {entry.getKey(), entry.getValue()};
                arraySet[i] = temp;
                i++;
            }
            Array array = dbConn.createArrayOf("text", arraySet);
            proc.setArray(2, array);
            proc.execute();
            proc.close();

            root.put("app", app);
            root.put("method", method);
            root.put("status", "ok");
            root.put("code", "200");
            try {
                CommandsHelp.submit(app, mapper.writeValueAsString(root),
                        correlationID, LOGGER);
            } catch (JsonGenerationException e) {
                //Logger.log(Level.SEVERE, e.getMessage(), e);
            } catch (JsonMappingException e) {
                //Logger.log(Level.SEVERE, e.getMessage(), e);
            } catch (IOException e) {
                //Logger.log(Level.SEVERE, e.getMessage(), e);
            }

        } catch (PSQLException e) {
            if (e.getMessage().contains("unique constraint")) {
                if (e.getMessage().contains("(username)"))
                    CommandsHelp.handleError(app, method, "Username already exists", correlationID, LOGGER);
                if (e.getMessage().contains("(email)"))
                    CommandsHelp.handleError(app, method, "Email already exists", correlationID, LOGGER);
            } else {
                CommandsHelp.handleError(app, method, e.getMessage(), correlationID, LOGGER);
            }

            //Logger.log(Level.SEVERE, e.getMessage(), e);
        } catch (SQLException e) {
            CommandsHelp.handleError(app, method, e.getMessage(), correlationID, LOGGER);
            //Logger.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            PostgresConnection.disconnect(null, proc, dbConn,null);
        }
    }
}
