package edumsg.core.commands.tweet;

import edumsg.core.Command;
import edumsg.core.CommandsHelp;
import edumsg.core.PostgresConnection;
import edumsg.redis.EduMsgRedis;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.json.JSONException;
import org.json.JSONObject;
import org.postgresql.util.PSQLException;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Types;
import java.util.logging.Logger;

/**
 * Created by Omar on 7/3/2016.
 */
public class ReplyCommand extends Command implements Runnable
{
    private final Logger LOGGER = Logger.getLogger(NewTweetCommand.class.getName());

    @Override
    public void execute() {
        try {
            dbConn = PostgresConnection.getDataSource().getConnection();
            dbConn.setAutoCommit(true);
            if (map.containsKey("image_url")) {
                proc = dbConn.prepareCall("{call reply(?,?,?,now()::timestamp,?)}");
            } else {
                proc = dbConn.prepareCall("{call reply(?,?,?,now()::timestamp)}");
            }

            proc.setPoolable(true);
            proc.setInt(1, Integer.parseInt(map.get("tweet_id")));
            proc.setString(2, map.get("tweet_text"));
            proc.setInt(3, Integer.parseInt(map.get("creator_id")));

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
                //Logger.log(Level.SEVERE, e.getMessage(), e);
            } catch (JsonMappingException e) {
                //Logger.log(Level.SEVERE, e.getMessage(), e);
            } catch (IOException e) {
                //Logger.log(Level.SEVERE, e.getMessage(), e);
            } catch (JSONException e) {
                e.printStackTrace();
            }

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
