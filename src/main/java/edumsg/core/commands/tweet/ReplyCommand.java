package edumsg.core.commands.tweet;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import edumsg.core.Command;
import edumsg.core.CommandsHelp;
import edumsg.core.PostgresConnection;
import edumsg.redis.Cache;
import edumsg.redis.ListCache;
import edumsg.redis.TweetsCache;
import edumsg.redis.UserCache;
import org.json.JSONObject;
import org.postgresql.util.PSQLException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
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
                proc = dbConn.prepareCall("{call reply(?,?,?,?,?)}");
            } else {
                proc = dbConn.prepareCall("{call reply(?,?,?,?)}");
            }

            proc.setPoolable(true);

            proc.setInt(1, Integer.parseInt(map.get("tweet_id")));
            proc.setString(2, map.get("tweet_text"));
            proc.setString(3, map.get("session_id"));
            proc.setString(4, map.get("type"));

            if (map.containsKey("image_url")) {
                proc.setString(4, map.get("image_url"));
            }

            proc.execute();

            root.put("app", map.get("app"));
            root.put("method", map.get("method"));
            root.put("status", "ok");
            root.put("code", "200");

            proc.close();

            CommandsHelp.submit(map.get("app"), mapper.writeValueAsString(root), map.get("correlation_id"), LOGGER);

            String sessionID = map.get("session_id");
            String type = map.get("type");

            String userTweetsCacheEntry = UserCache.userCache.get("user_tweets_" + type + ":" + sessionID);
            String timelineCacheEntry = UserCache.userCache.get("timeline_" + type + ":" + map.get("session_id"));
            String earliestRepliesCacheEntry = TweetsCache.tweetCache.get("get_earliest_replies:" + map.get("session_id"));
            String repliesCacheEntry = TweetsCache.tweetCache.get("get_replies:" + map.get("session_id"));
            String listFeedsCacheEntry = ListCache.listCache.get("get_list_feeds:" + map.get("session_id"));

            CommandsHelp.invalidateCacheEntry(UserCache.userCache,userTweetsCacheEntry,"user_tweets_", sessionID, type);
            CommandsHelp.invalidateCacheEntry(UserCache.userCache,timelineCacheEntry,"timeline_", sessionID, type);
            CommandsHelp.invalidateCacheEntry(TweetsCache.tweetCache, repliesCacheEntry,"get_earliest_replies",sessionID);
            CommandsHelp.invalidateCacheEntry(TweetsCache.tweetCache, earliestRepliesCacheEntry,"get_replies",sessionID);
            CommandsHelp.invalidateCacheEntry(ListCache.listCache, listFeedsCacheEntry,"get_list_feeds",sessionID);


        } catch ( Exception e ) {

            String app = map.get("app");
            String method = map.get("method");
            String errMsg = CommandsHelp.getErrorMessage(app, method, e);

            CommandsHelp.handleError(app, method, errMsg, map.get("correlation_id"), LOGGER);
            LOGGER.log(Level.SEVERE, e.getMessage(), e);

        } finally {
            PostgresConnection.disconnect(null, proc, dbConn);
        }
    }
}
