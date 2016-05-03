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

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import edumsg.core.Command;
import edumsg.core.CommandsHelp;
import edumsg.core.PostgresConnection;
import org.postgresql.util.PSQLException;

import java.io.IOException;
import java.sql.SQLException;
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

            if (map.containsKey("image_url")) {
                proc = dbConn.prepareCall("{? = call create_dm(?,?,?,now()::timestamp,?)}");
            } else {
                proc = dbConn.prepareCall("{? = call create_dm(?,?,?,now()::timestamp)}");
            }
            proc.setPoolable(true);
            proc.registerOutParameter(1, Types.BOOLEAN);
            proc.setString(2, map.get("session_id"));
            proc.setInt(3, Integer.parseInt(map.get("receiver_id")));
            proc.setString(4, map.get("dm_text"));
            if (map.containsKey("image_url"))
                proc.setString(5, map.get("image_url"));
            proc.execute();

            boolean sent = proc.getBoolean(1);
            if (sent) {
                root.put("app", map.get("app"));
                root.put("method", map.get("method"));
                root.put("status", "ok");
                root.put("code", "200");
                try {
                    CommandsHelp.submit(map.get("app"), mapper.writeValueAsString(root), map.get("correlation_id"), LOGGER);
//                    String cacheEntry = Cache.userCache.get("get_conv");
//                    if (cacheEntry != null) {
//                        JSONObject cacheEntryJson = new JSONObject(cacheEntry);
//                        cacheEntryJson.put("cacheStatus", "invalid");
////                    System.out.println("invalidated");
//                        Cache.userCache.set("get_conv", cacheEntryJson.toString());
//                    }
//                    String cacheEntry1 = Cache.userCache.get("get_convs");
//                    if (cacheEntry1 != null) {
//                        JSONObject cacheEntryJson = new JSONObject(cacheEntry1);
//                        cacheEntryJson.put("cacheStatus", "invalid");
////                    System.out.println("invalidated");
//                        Cache.userCache.set("get_convs", cacheEntryJson.toString());
//                    }
                } catch (JsonGenerationException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                } catch (JsonMappingException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
//                catch (JSONException e) {
//                    e.printStackTrace();
//                }
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
