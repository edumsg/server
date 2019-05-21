package edumsg.core.commands.user;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import edumsg.core.Command;
import edumsg.core.CommandsHelp;
import edumsg.core.PostgresConnection;
import org.postgresql.util.PSQLException;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* Created by Omar on 7/3/2016.
*/
public class GetRetweetsCommand extends Command implements Runnable
{
    private final Logger LOGGER = Logger.getLogger(GetUserCommand.class.getName());

    @Override
    public void execute() {

        try {
            dbConn = PostgresConnection.getDataSource().getConnection();
            dbConn.setAutoCommit(false);

            proc = dbConn.prepareCall("{? = call get_retweets_ids(?)}");
            proc.setPoolable(true);

            proc.registerOutParameter(1, Types.OTHER);
            proc.setString(2, map.get("session_id"));

            proc.execute();
            set = (ResultSet) proc.getObject(1);

            ArrayNode tweetIds = nf.arrayNode();

            while (set.next()) {
                tweetIds.add(set.getInt(1));
            }

            root.put("app", map.get("app"));
            root.put("method", map.get("method"));
            root.put("status", "ok");
            root.put("code", "200");

            proc.close();
            set.close();

            root.set("tweet_ids", tweetIds);

            CommandsHelp.submit(map.get("app"), mapper.writeValueAsString(root), map.get("correlation_id"), LOGGER);

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
