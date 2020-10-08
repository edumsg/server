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

package edumsg.core.commands.list;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import edumsg.core.Command;
import edumsg.core.CommandsHelp;
import edumsg.core.PostgresConnection;
import edumsg.redis.Cache;
import edumsg.redis.ListCache;
import org.json.JSONObject;
import org.postgresql.util.PSQLException;

import java.io.IOException;
import java.sql.Array;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UpdateListCommand extends Command implements Runnable {
    private final Logger LOGGER = Logger.getLogger(UpdateListCommand.class.getName());
    private static double classVersion = 1.0;

    @Override
    public void execute() {
        String app = map.get("app");
        String method = map.get("method");
        String correlationID = map.get("correlation_id");
        try {
            dbConn = PostgresConnection.getDataSource().getConnection();
            dbConn.setAutoCommit(true);
            proc = dbConn.prepareCall("{call update_list(?,?)}");
            proc.setPoolable(true);
            proc.setInt(1, Integer.parseInt(map.get("list_id")));

            map.remove("list_id");
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

            root.put("app", app);
            root.put("method", method);
            root.put("status", "ok");
            root.put("code", "200");
            try {
                CommandsHelp.submit(app, mapper.writeValueAsString(root),
                        correlationID, LOGGER);
                String cacheEntry = ListCache.listCache.get("get_list:" + map.get("session_id"));
                if (cacheEntry != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry);
                    cacheEntryJson.put("cacheStatus", "invalid");
//                    System.out.println("invalidated");
                    ListCache.listCache.set("get_list:" + map.get("session_id"), cacheEntryJson.toString());
                }
                String cacheEntry1 =   ListCache.listCache.get("get_list_feeds:" + map.get("session_id"));
                if (cacheEntry1 != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry1);
                    cacheEntryJson.put("cacheStatus", "invalid");
//                    System.out.println("invalidated");
                    ListCache.listCache.set("get_list_feeds:" + map.get("session_id"), cacheEntryJson.toString());
                }
                String cacheEntry2 =   ListCache.listCache.get("list_members:" + map.get("session_id"));
                if (cacheEntry2 != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry2);
                    cacheEntryJson.put("cacheStatus", "invalid");
//                    System.out.println("invalidated");
                    ListCache.listCache.set("list_members:" + map.get("session_id"), cacheEntryJson.toString());
                }
                String cacheEntry3 =   ListCache.listCache.get("list_subscribers:" + map.get("session_id"));
                if (cacheEntry3 != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry3);
                    cacheEntryJson.put("cacheStatus", "invalid");
//                    System.out.println("invalidated");
                    ListCache.listCache.set("list_subscribers:" + map.get("session_id"), cacheEntryJson.toString());
                }
            } catch (JsonGenerationException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            } catch (JsonMappingException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }

        } catch (PSQLException e) {
            if (e.getMessage().contains("unique constraint")) {
                if (e.getMessage().contains("(name)")) {
                    CommandsHelp.handleError(app, method, "List name already exists", correlationID, LOGGER);
                }
            }
            if (e.getMessage().contains("value too long")) {
                CommandsHelp.handleError(app, method, "Too long input", correlationID, LOGGER);
            }
            CommandsHelp.handleError(app, method, "List name already exists", correlationID, LOGGER);
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } catch (SQLException e) {
            CommandsHelp.handleError(app, method, e.getMessage(), correlationID, LOGGER);
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            PostgresConnection.disconnect(null, proc, dbConn);
        }
    }

    public static double getClassVersion() {
        return classVersion;
    }
}
