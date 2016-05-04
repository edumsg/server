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

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import edumsg.core.Command;
import edumsg.core.CommandsHelp;
import edumsg.core.PostgresConnection;
import edumsg.redis.Cache;
import org.json.JSONObject;
import org.postgresql.util.PSQLException;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RetweetCommand extends Command implements Runnable {
    private final Logger LOGGER = Logger.getLogger(RetweetCommand.class.getName());

    @Override
    public void execute() {
        try {
            dbConn = PostgresConnection.getDataSource().getConnection();
            dbConn.setAutoCommit(true);
            proc = dbConn.prepareCall("{? = call retweet(?,?)}");
            proc.setPoolable(true);
            proc.registerOutParameter(1, Types.INTEGER);
            proc.setInt(2, Integer.parseInt(map.get("tweet_id")));
            proc.setString(3, map.get("session_id"));
            proc.execute();

            int retweets = proc.getInt(1);

            root.put("app", map.get("app"));
            root.put("method", map.get("method"));
            root.put("status", "ok");
            root.put("code", "200");
            root.put("retweet_count", retweets);
            try {
                CommandsHelp.submit(map.get("app"), mapper.writeValueAsString(root), map.get("correlation_id"), LOGGER);
                String cacheEntry = Cache.userCache.get("user_tweets:" + map.get("session_id"));
                if (cacheEntry != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry);
                    cacheEntryJson.put("cacheStatus", "invalid");
//                    System.out.println("invalidated");
                    Cache.userCache.set("user_tweets:" + map.get("session_id"), cacheEntryJson.toString());
                }
                String cacheEntry1 = Cache.userCache.get("timeline:" + map.get("session_id"));
                if (cacheEntry1 != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry1);
                    cacheEntryJson.put("cacheStatus", "invalid");
//                    System.out.println("invalidated");
                    Cache.userCache.set("timeline:" + map.get("session_id"), cacheEntryJson.toString());
                }
                String cacheEntry2 = Cache.tweetCache.get("get_earliest_replies:" + map.get("session_id"));
                if (cacheEntry2 != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry2);
                    cacheEntryJson.put("cacheStatus", "invalid");
//                    System.out.println("invalidated");
                    Cache.tweetCache.set("get_earliest_replies:" + map.get("session_id"), cacheEntryJson.toString());
                }
                String cacheEntry3 = Cache.tweetCache.get("get_replies:" + map.get("session_id"));
                if (cacheEntry3 != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry3);
                    cacheEntryJson.put("cacheStatus", "invalid");
//                    System.out.println("invalidated");
                    Cache.tweetCache.set("get_replies:" + map.get("session_id"), cacheEntryJson.toString());
                }
                String cacheEntry4 = Cache.listCache.get("get_list_feeds:" + map.get("session_id"));
                if (cacheEntry4 != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry4);
                    cacheEntryJson.put("cacheStatus", "invalid");
//                    System.out.println("invalidated");
                    Cache.listCache.set("get_list_feeds:" + map.get("session_id"), cacheEntryJson.toString());
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

        } catch (PSQLException e) {
            if (e.getMessage().contains("unique constraint")) {
                CommandsHelp.handleError(map.get("app"), map.get("method"), "You already retweeted this tweet", map.get("correlation_id"), LOGGER);
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
