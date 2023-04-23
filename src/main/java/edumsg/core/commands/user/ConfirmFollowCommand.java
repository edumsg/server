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

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import edumsg.NodeManager.Main;
import edumsg.core.Command;
import edumsg.core.CommandsHelp;
import edumsg.core.PostgresConnection;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfirmFollowCommand extends Command implements Runnable {
    private static double classVersion = 1.0;
    private final Logger LOGGER = Logger.getLogger(ConfirmFollowCommand.class.getName());

    public static double getClassVersion() {
        return classVersion;
    }

    @Override
    public void execute() {

        try {
            dbConn = PostgresConnection.getDataSource().getConnection();
            dbConn.setAutoCommit(true);
            proc = dbConn.prepareCall("{call confirm_follow(?,?)}");
            proc.setPoolable(true);
            proc.setString(1, map.get("session_id"));
            proc.setInt(2, Integer.parseInt(map.get("follower_id")));
            proc.execute();

            root.put("app", map.get("app"));
            root.put("method", map.get("method"));
            root.put("status", "ok");
            root.put("code", "200");


            proc.close();

            try {
                CommandsHelp.submit(map.get("app"),
                        mapper.writeValueAsString(root), map.get("correlation_id"), LOGGER);
                String cacheEntry = Main.userCache.jedisCache.get("user_tweets:" + map.get("session_id"));
                if (cacheEntry != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry);
                    cacheEntryJson.put("cacheStatus", "invalid");
//                    System.out.println("invalidated");
                    Main.userCache.jedisCache.set("user_tweets:" + map.get("session_id"), cacheEntryJson.toString());
                }
                String cacheEntry1 = Main.userCache.jedisCache.get("timeline:" + map.get("session_id"));
                if (cacheEntry1 != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry1);
                    cacheEntryJson.put("cacheStatus", "invalid");
//                    System.out.println("invalidated");
                    Main.userCache.jedisCache.set("timeline:" + map.get("session_id"), cacheEntryJson.toString());
                }
                String cacheEntry2 = Main.userCache.jedisCache.get("followers:" + map.get("session_id"));
                if (cacheEntry2 != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry2);
                    cacheEntryJson.put("cacheStatus", "invalid");
//                    System.out.println("invalidated");
                    Main.userCache.jedisCache.set("followers:" + map.get("session_id"), cacheEntryJson.toString());
                }
                String cacheEntry3 = Main.userCache.jedisCache.get("following:" + map.get("session_id"));
                if (cacheEntry3 != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry3);
                    cacheEntryJson.put("cacheStatus", "invalid");
//                    System.out.println("invalidated");
                    Main.userCache.jedisCache.set("following:" + map.get("session_id"), cacheEntryJson.toString());
                }
                String cacheEntry4 = Main.listCache.jedisCache.get("get_list_feeds:" + map.get("session_id"));
                if (cacheEntry4 != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry4);
                    cacheEntryJson.put("cacheStatus", "invalid");
//                    System.out.println("invalidated");
                    Main.listCache.jedisCache.set("get_list_feeds:" + map.get("session_id"), cacheEntryJson.toString());
                }
            } catch (JsonGenerationException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            } catch (JsonMappingException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
//            catch (JSONException e) {
//                e.printStackTrace();
//            }

        } catch (SQLException e) {
            CommandsHelp.handleError(map.get("app"), map.get("method"), e.getMessage(), map.get("correlation_id"), LOGGER);
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            PostgresConnection.disconnect(null, proc, dbConn, null);
        }
    }
}
