package edumsg.core.commands.tweet;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import edumsg.core.commands.user.GetUserCommand;
import org.postgresql.util.PSQLException;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;

import edumsg.core.Command;
import edumsg.core.CommandsHelp;
import edumsg.core.PostgresConnection;
import edumsg.core.Tweet;
import edumsg.core.User;
import edumsg.core.commands.user.GetUserCommand;

/**
 * Created by omarelhagin on 15/3/16.
 */
public class GetRepliesCommand extends Command implements Runnable
{
    private final Logger LOGGER = Logger.getLogger(GetUserCommand.class.getName());

    @Override
    public void execute() {

        try {
            dbConn = PostgresConnection.getDataSource().getConnection();
            dbConn.setAutoCommit(false);
            proc = dbConn.prepareCall("{? = call get_replies(?)}");
            proc.setPoolable(true);
            proc.registerOutParameter(1, Types.OTHER);
            proc.setInt(2, Integer.parseInt(map.get("tweet_id")));
            proc.execute();

            set = (ResultSet) proc.getObject(1);

            ArrayNode tweets = nf.arrayNode();
            root.put("app", map.get("app"));
            root.put("method", map.get("method"));
            root.put("status", "ok");
            root.put("code", "200");

            while (set.next()) {
                Integer creatorId = set.getInt(1);
                String username = set.getString(2);
                String name = set.getString(3);
                String avatarUrl = set.getString(4);
                Integer replyId = set.getInt(5);
                String tweetText = set.getString(6);
                String tweetImgUrl = set.getString(7);
                Timestamp createdAt = set.getTimestamp(8);
                User creator = new User();
                creator.setId(creatorId);
                creator.setUsername(username);
                creator.setName(name);
                creator.setAvatarUrl(avatarUrl);

                Tweet t = new Tweet();
                t.setId(replyId);
                t.setTweetText(tweetText);
                t.setImageUrl(tweetImgUrl);
                t.setCreator(creator);
                t.setCreatedAt(createdAt);

                tweets.addPOJO(t);
            }

            root.set("replies", tweets);
            try {
                CommandsHelp.submit(map.get("app"),
                        mapper.writeValueAsString(root),
                        map.get("correlation_id"), LOGGER);
            } catch (JsonGenerationException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            } catch (JsonMappingException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }

            dbConn.commit();
        } catch (PSQLException e) {
            CommandsHelp.handleError(map.get("app"), map.get("method"), e.getMessage(), map.get("correlation_id"), LOGGER);
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } catch (SQLException e) {
            CommandsHelp.handleError(map.get("app"), map.get("method"), e.getMessage(), map.get("correlation_id"), LOGGER);
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            PostgresConnection.disconnect(null, proc, dbConn);
        }
    }
}