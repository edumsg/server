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

package edumsg.core.commands.tweet;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.logging.Logger;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.json.JSONException;
import org.json.JSONObject;
import org.postgresql.util.PSQLException;

import edumsg.core.Command;
import edumsg.core.CommandsHelp;
import edumsg.core.PostgresConnection;
import edumsg.redis.EduMsgRedis;
import edumsg.shared.MyObjectMapper;

public class UnFavoriteCommand extends Command implements Runnable {
    private final Logger LOGGER = Logger.getLogger(UnFavoriteCommand.class.getName());

    @Override
    public void execute() {
        try {
            dbConn = PostgresConnection.getDataSource().getConnection();
            dbConn.setAutoCommit(true);
            proc = dbConn.prepareCall("{? = call unfavorite(?,?)}");
            proc.setPoolable(true);
            proc.registerOutParameter(1, Types.INTEGER);
            proc.setInt(2, Integer.parseInt(map.get("tweet_id")));
            proc.setInt(3, Integer.parseInt(map.get("user_id")));
            proc.execute();

            int favorites = proc.getInt(1);

            root.put("app", map.get("app"));
            root.put("method", map.get("method"));
            root.put("status", "ok");
            root.put("code", "200");
            root.put("favorites", favorites);
            try {
                CommandsHelp.submit(map.get("app"),
                        mapper.writeValueAsString(root),
                        map.get("correlation_id"), LOGGER);
                String cacheEntry = EduMsgRedis.redisCache.get("timeline");
                if (cacheEntry != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry);
                    cacheEntryJson.put("cacheStatus", "invalid");
                    System.out.println("invalidated");
                    EduMsgRedis.redisCache.set("timeline", cacheEntryJson.toString());
                }
                String cacheEntry1 = EduMsgRedis.redisCache.get("get_feeds");
                if (cacheEntry1 != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry1);
                    cacheEntryJson.put("cacheStatus", "invalid");
                    System.out.println("invalidated");
                    EduMsgRedis.redisCache.set("get_feeds", cacheEntryJson.toString());
                }
            } catch (JsonGenerationException e) {
                //LOGGER.log(Level.OFF, e.getMessage(), e);
            } catch (JsonMappingException e) {
                //LOGGER.log(Level.OFF, e.getMessage(), e);
            } catch (IOException e) {
                //LOGGER.log(Level.OFF, e.getMessage(), e);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } catch (PSQLException e) {
            CommandsHelp.handleError(map.get("app"), map.get("method"), e.getMessage(), map.get("correlation_id"), LOGGER);
            //LOGGER.log(Level.OFF, e.getMessage(), e);
        } catch (SQLException e) {
            CommandsHelp.handleError(map.get("app"), map.get("method"), e.getMessage(), map.get("correlation_id"), LOGGER);
            //LOGGER.log(Level.OFF, e.getMessage(), e);
        } finally {
            PostgresConnection.disconnect(null, proc, dbConn);
        }
    }
}
