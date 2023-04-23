package edumsg.core.commands.tweet;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import edumsg.core.Command;
import edumsg.core.CommandsHelp;
import edumsg.core.PostgresConnection;
import edumsg.redis.ListCache;
import edumsg.redis.TweetsCache;
import edumsg.redis.UserCache;
import org.json.JSONObject;
import org.postgresql.util.PSQLException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Created by Omar on 7/3/2016.
 */
public class ReplyCommand extends Command implements Runnable {
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
            if (map.containsKey("image_url")) {
                proc = dbConn.prepareCall("{call reply(?,?,?,?)}");
            } else {
                proc = dbConn.prepareCall("{call reply(?,?,?)}");
            }

            proc.setPoolable(true);
            proc.setInt(1, Integer.parseInt(map.get("tweet_id")));
            proc.setString(2, map.get("tweet_text"));
            proc.setString(3, map.get("session_id"));

            if (map.containsKey("image_url")) {
                proc.setString(4, map.get("image_url"));
            }

            proc.execute();

            root.put("app", map.get("app"));
            root.put("method", map.get("method"));
            root.put("status", "ok");
            root.put("code", "200");
            try {
                CommandsHelp.submit(map.get("app"), mapper.writeValueAsString(root), map.get("correlation_id"), LOGGER);
                String cacheEntry = UserCache.userCache.get("user_tweets:" + map.get("session_id"));
                if (cacheEntry != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry);
                    cacheEntryJson.put("cacheStatus", "invalid");
//                    System.out.println("invalidated");
                    UserCache.userCache.set("user_tweets:" + map.get("session_id"), cacheEntryJson.toString());
                }
                String cacheEntry1 = UserCache.userCache.get("timeline:" + map.get("session_id"));
                if (cacheEntry1 != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry1);
                    cacheEntryJson.put("cacheStatus", "invalid");
//                    System.out.println("invalidated");
                    UserCache.userCache.set("timeline:" + map.get("session_id"), cacheEntryJson.toString());
                }
                String cacheEntry2 = TweetsCache.tweetCache.get("get_earliest_replies:" + map.get("session_id"));
                if (cacheEntry2 != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry2);
                    cacheEntryJson.put("cacheStatus", "invalid");
//                    System.out.println("invalidated");
                    TweetsCache.tweetCache.set("get_earliest_replies:" + map.get("session_id"), cacheEntryJson.toString());
                }
                String cacheEntry3 = TweetsCache.tweetCache.get("get_replies:" + map.get("session_id"));
                if (cacheEntry3 != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry3);
                    cacheEntryJson.put("cacheStatus", "invalid");
//                    System.out.println("invalidated");
                    TweetsCache.tweetCache.set("get_replies:" + map.get("session_id"), cacheEntryJson.toString());
                }
                String cacheEntry4 = ListCache.listCache.get("get_list_feeds:" + map.get("session_id"));
                if (cacheEntry4 != null) {
                    JSONObject cacheEntryJson = new JSONObject(cacheEntry4);
                    cacheEntryJson.put("cacheStatus", "invalid");
//                    System.out.println("invalidated");
                    ListCache.listCache.set("get_list_feeds:" + map.get("session_id"), cacheEntryJson.toString());
                }
            } catch (JsonGenerationException e) {
                //Logger.log(Level.SEVERE, e.getMessage(), e);
            } catch (JsonMappingException e) {
                //Logger.log(Level.SEVERE, e.getMessage(), e);
            } catch (IOException e) {
                //Logger.log(Level.SEVERE, e.getMessage(), e);
            }
//            catch (JSONException e) {
//                e.printStackTrace();
//            }

        } catch (PSQLException e) {
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
