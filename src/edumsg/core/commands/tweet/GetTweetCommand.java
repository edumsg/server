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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.logging.Logger;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.POJONode;
import org.postgresql.util.PSQLException;

import edumsg.core.Command;
import edumsg.core.CommandsHelp;
import edumsg.core.PostgresConnection;
import edumsg.core.Tweet;
import edumsg.core.User;
import edumsg.shared.MyObjectMapper;

public class GetTweetCommand extends Command implements Runnable {
    private final Logger LOGGER = Logger.getLogger(GetTweetCommand.class.getName());

    @Override
    public void execute() {
        try {
            dbConn = PostgresConnection.getDataSource().getConnection();
            dbConn.setAutoCommit(false);
            proc = dbConn.prepareCall("{? = call get_tweet(?)}");
            proc.setPoolable(true);
            proc.registerOutParameter(1, Types.OTHER);
            proc.setInt(2, Integer.parseInt(map.get("tweet_id")));
            proc.execute();

            set = (ResultSet) proc.getObject(1);

            root.put("app", map.get("app"));
            root.put("method", map.get("method"));
            root.put("status", "ok");
            root.put("code", "200");

            Tweet t = new Tweet();
            if (set.next()) {
                Integer id = set.getInt(1);
                String tweet = set.getString(2);
                String image_url = set.getString(5);
                Timestamp created_at = set.getTimestamp(4);
                String creator_username = set.getString(6);
                String creator_name = set.getString(7);
                String creator_avatar = set.getString(8);
                int retweets = set.getInt(9);
                int favorites = set.getInt(10);

                t.setId(id);
                t.setTweetText(tweet);
                t.setImageUrl(image_url);
                t.setCreatedAt(created_at);
                t.setRetweets(retweets);
                t.setFavorites(favorites);
                User creator = new User();
                creator.setName(creator_name);
                creator.setAvatarUrl(creator_avatar);
                creator.setUsername(creator_username);
                t.setCreator(creator);
            }

            POJONode child = nf.POJONode(t);
            root.put("tweet", child);
            try {
                CommandsHelp.submit(map.get("app"),
                        mapper.writeValueAsString(root),
                        map.get("correlation_id"), LOGGER);
            } catch (JsonGenerationException e) {
                //Logger.log(Level.SEVERE, e.getMessage(), e);
            } catch (JsonMappingException e) {
                //Logger.log(Level.SEVERE, e.getMessage(), e);
            } catch (IOException e) {
                //Logger.log(Level.SEVERE, e.getMessage(), e);
            }

            dbConn.commit();
        } catch (PSQLException e) {
            CommandsHelp.handleError(map.get("app"), map.get("method"), e.getMessage(), map.get("correlation_id"), LOGGER);
            //Logger.log(Level.SEVERE, e.getMessage(), e);
        } catch (SQLException e) {
            CommandsHelp.handleError(map.get("app"), map.get("method"), e.getMessage(), map.get("correlation_id"), LOGGER);
            //Logger.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            PostgresConnection.disconnect(set, proc, dbConn);
        }
    }
}
