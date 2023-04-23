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
import java.util.logging.Logger;

/**
 * Created by omarelhagin on 21/5/16.
 */
public class GetUserTweets2Command extends Command implements Runnable {
    private static double classVersion = 1.0;
    private final Logger LOGGER = Logger.getLogger(GetUserTweetsCommand.class.getName());

    public static double getClassVersion() {
        return classVersion;
    }

    @Override
    public void execute() {

        try {
            dbConn = PostgresConnection.getDataSource().getConnection();
            dbConn.setAutoCommit(false);
            proc = dbConn.prepareCall("{? = call get_tweets2(?)}");
            proc.setPoolable(true);
            proc.registerOutParameter(1, Types.OTHER);
            proc.setString(2, map.get("username"));
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
                String image_url = set.getString(3);
                Timestamp created_at = set.getTimestamp(4);
                Integer creator_id = set.getInt(5);
                String creator_name = set.getString(6);
                String creator_username = set.getString(7);
                String creator_avatar = set.getString(8);

                Tweet t = new Tweet();
                t.setId(id);
                t.setTweetText(tweet);
                t.setImageUrl(image_url);
                t.setCreatedAt(created_at);
                User creator = new User();
                creator.setId(creator_id);
                creator.setName(creator_name);
                creator.setAvatarUrl(creator_avatar);
                creator.setUsername(creator_username);
                t.setCreator(creator);

                tweets.addPOJO(t);
            }

//            set.close();
//            proc.close();
            root.set("tweets", tweets);
            try {
                CommandsHelp.submit(map.get("app"),
                        mapper.writeValueAsString(root),
                        map.get("correlation_id"), LOGGER);
                JSONObject cacheEntry = new JSONObject(mapper.writeValueAsString(root));
                cacheEntry.put("cacheStatus", "valid");
                UserCache.userCache.set("user_tweets:" + map.get("session_id"), cacheEntry.toString());
            } catch (JsonGenerationException e) {
                //Logger.log(Level.SEVERE, e.getMessage(), e);
            } catch (JsonMappingException e) {
                //Logger.log(Level.SEVERE, e.getMessage(), e);
            } catch (IOException e) {
                //Logger.log(Level.SEVERE, e.getMessage(), e);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            dbConn.commit();
        } catch (PSQLException e) {
            CommandsHelp.handleError(map.get("app"), map.get("method"), e.getMessage(), map.get("correlation_id"), LOGGER);
            //Logger.log(Level.SEVERE, e.getMessage(), e);
        } catch (SQLException e) {
            CommandsHelp.handleError(map.get("app"), map.get("method"), e.getMessage(), map.get("correlation_id"), LOGGER);
            //Logger.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            PostgresConnection.disconnect(set, proc, dbConn, null);
        }
    }
}
