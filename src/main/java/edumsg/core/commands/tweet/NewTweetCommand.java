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
import edumsg.NodeManager.Main;
import edumsg.core.Command;
import edumsg.core.CommandsHelp;
import edumsg.core.PostgresConnection;
import edumsg.redis.ListCache;
import edumsg.redis.TweetCache;
import edumsg.redis.UserCache;
import org.json.JSONObject;
import org.postgresql.util.PSQLException;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class NewTweetCommand extends Command implements Runnable {
    private static double classVersion = 1.0;
    private final Logger LOGGER = Logger.getLogger(NewTweetCommand.class.getName());

    public static double getClassVersion() {
        return classVersion;
    }

    @Override
    public void execute() {
        try {
            dbConn = PostgresConnection.getDataSource().getConnection();
            dbConn.setAutoCommit(true);
            Statement query = dbConn.createStatement();
            query.setPoolable(true);
            if (map.containsKey("image_url")) {

                set = query.executeQuery(String.format("SELECT * FROM create_tweet('%s','%s','%s')",
                        map.get("tweet_text"),
                        map.get("session_id"),
                        map.get("image_url")));
            } else {
                set = query.executeQuery(String.format("SELECT * FROM create_tweet('%s','%s')",
                        map.get("tweet_text"),
                        map.get("session_id")));

            }
            String id = null;
            while (set.next()) {
                System.out.println("creator_id..." + set.getInt("creator_id"));
                id = set.getInt("id") + "";
                details.put("id", id);
                details.put("tweet_text", set.getString("tweet_text"));
                details.put("creator_id", set.getInt("creator_id") + "");
                details.put("image_url", set.getString("image_url"));
                details.put("created_at", set.getTimestamp("created_at") + "");
                //Cache.cacheTweet(set.getInt("id")+"", details);
                //Cache.cacheUserTweet(map.get("creator_id"),set.getInt("id")+"");
                root.put("id", set.getInt("id"));
            }


            root.put("app", map.get("app"));
            root.put("method", map.get("method"));
            root.put("status", "ok");
            root.put("code", "200");
            set.close();

            try {
                CommandsHelp.submit(map.get("app"), mapper.writeValueAsString(root), map.get("correlation_id"), LOGGER);
                String cacheEntry = ((UserCache) Main.cacheMap.get("user")).jedisCache.get("user_tweets:" + map.get("session_id"));
                if (cacheEntry != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry);
                    cacheEntryJson.put("cacheStatus", "invalid");
//                    System.out.println("invalidated");
                    ((UserCache) Main.cacheMap.get("user")).jedisCache.set("user_tweets:" + map.get("session_id"), cacheEntryJson.toString());
                }
                String cacheEntry1 = ((UserCache) Main.cacheMap.get("user")).jedisCache.get("timeline:" + map.get("session_id"));
                if (cacheEntry1 != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry1);
                    cacheEntryJson.put("cacheStatus", "invalid");
//                    System.out.println("invalidated");
                    ((UserCache) Main.cacheMap.get("user")).jedisCache.set("timeline:" + map.get("session_id"), cacheEntryJson.toString());
                }
                String cacheEntry2 = ((TweetCache) Main.cacheMap.get("tweet")).jedisCache.get("get_earliest_replies:" + map.get("session_id"));
                if (cacheEntry2 != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry2);
                    cacheEntryJson.put("cacheStatus", "invalid");
//                    System.out.println("invalidated");
                    ((TweetCache) Main.cacheMap.get("tweet")).jedisCache.set("get_earliest_replies:" + map.get("session_id"), cacheEntryJson.toString());
                }
                String cacheEntry3 = ((TweetCache) Main.cacheMap.get("tweet")).jedisCache.get("get_replies:" + map.get("session_id"));
                if (cacheEntry3 != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry3);
                    cacheEntryJson.put("cacheStatus", "invalid");
//                    System.out.println("invalidated");
                    ((TweetCache) Main.cacheMap.get("tweet")).jedisCache.set("get_replies:" + map.get("session_id"), cacheEntryJson.toString());
                }
                String cacheEntry4 = ((ListCache) Main.cacheMap.get("list")).jedisCache.get("get_list_feeds:" + map.get("session_id"));
                if (cacheEntry4 != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry4);
                    cacheEntryJson.put("cacheStatus", "invalid");
//                    System.out.println("invalidated");
                    ((ListCache) Main.cacheMap.get("list")).jedisCache.set("get_list_feeds:" + map.get("session_id"), cacheEntryJson.toString());
                }
            } catch (JsonGenerationException e) {
                //Logger.log(Level.SEVERE, e.getMessage(), e);
            } catch (JsonMappingException e) {
                //Logger.log(Level.SEVERE, e.getMessage(), e);
            } catch (IOException e) {
                //Logger.log(Level.SEVERE, e.getMessage(), e);
            }
//            catch (JSONException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }

        } catch (PSQLException e) {
            System.out.println("Exception..." + e.getMessage());
            if (e.getMessage().contains("value too long")) {
                CommandsHelp.handleError(map.get("app"), map.get("method"), "Tweet exceeds 140 characters", map.get("correlation_id"), LOGGER);
            } else {
                CommandsHelp.handleError(map.get("app"), map.get("method"), e.getMessage(), map.get("correlation_id"), LOGGER);
            }
            //Logger.log(Level.SEVERE, e.getMessage(), e);
        } catch (SQLException e) {
            CommandsHelp.handleError(map.get("app"), map.get("method"), e.getMessage(), map.get("correlation_id"), LOGGER);
            //Logger.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            PostgresConnection.disconnect(null, proc, dbConn);
        }
    }
}
