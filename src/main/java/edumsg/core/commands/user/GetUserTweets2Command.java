package edumsg.core.commands.user;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import edumsg.core.*;
import edumsg.redis.UserCache;
import org.json.JSONException;
import org.json.JSONObject;
import org.postgresql.util.PSQLException;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by omarelhagin on 21/5/16.
 */
public class GetUserTweets2Command extends Command implements Runnable {
    private final Logger LOGGER = Logger.getLogger(GetUserTweetsCommand.class.getName());

    @Override
    public void execute() {

        try {
            dbConn = PostgresConnection.getDataSource().getConnection();
            dbConn.setAutoCommit(false);

            proc = dbConn.prepareCall("{? = call get_tweets2(?,?)}");
            proc.setPoolable(true);

            proc.registerOutParameter(1, Types.OTHER);
            proc.setString(2, map.get("username"));
            proc.setString(3,map.get("type"));

            proc.execute();
            set = (ResultSet) proc.getObject(1);

            ArrayNode tweets = nf.arrayNode();
            root.put("app", map.get("app"));
            root.put("method", map.get("method"));
            root.put("status", "ok");
            root.put("code", "200");

            while (set.next()) {
                Integer id = set.getInt(1);
                String tweet = set.getString(2);
                Timestamp created_at = set.getTimestamp(3);
                String type = set.getString(4);
                String image_url = set.getString(5);
                Integer creator_id = set.getInt(6);
                String creator_name = set.getString(7);
                String creator_username = set.getString(8);
                String creator_avatar = set.getString(9);

                Tweet t = new Tweet();
                t.setId(id);
                t.setTweetText(tweet);
                t.setImageUrl(image_url);
                t.setType(type);
                t.setCreatedAt(created_at);

                User creator = new User();
                creator.setId(creator_id);
                creator.setName(creator_name);
                creator.setAvatarUrl(creator_avatar);
                creator.setUsername(creator_username);

                t.setCreator(creator);

                tweets.addPOJO(t);
            }

            proc.close();
            set.close();
            root.set("tweets", tweets);

            // Submitting Response
            CommandsHelp.submit(map.get("app"), mapper.writeValueAsString(root), map.get("correlation_id"), LOGGER);

            // Caching Data
            String sessionID = map.get("session_id");
            String type = map.getOrDefault("type","rt");

            String userTweetsCacheEntry = UserCache.userCache.get("user_tweets_" + type + ":" + sessionID);

            CommandsHelp.validateCacheEntry(UserCache.userCache, userTweetsCacheEntry,"user_tweets_", sessionID, type);

            dbConn.commit();

        } catch ( Exception e ) {

            String app = map.get("app");
            String method = map.get("method");
            String errMsg = CommandsHelp.getErrorMessage(app, method, e);

            CommandsHelp.handleError(app, method, errMsg, map.get("correlation_id"), LOGGER);
            LOGGER.log(Level.SEVERE, e.getMessage(), e);

        } finally {
            PostgresConnection.disconnect(set, proc, dbConn, null);
        }
    }
}
