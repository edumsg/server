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
import com.fasterxml.jackson.databind.node.ValueNode;
import edumsg.core.*;
import org.postgresql.util.PSQLException;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.logging.Logger;

public class GetTweetCommand extends Command implements Runnable {
    private final Logger LOGGER = Logger.getLogger(GetTweetCommand.class.getName());

    @Override
    public void execute() {

        try {
            Tweet t = new Tweet();
            User creator = new User();
            details = null; //Cache.returnTweet(map.get("tweet_id"));

            if(details == null) {

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

                if (set.next()) {
                    details =  new HashMap<>();
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
                    creator.setName(creator_name);
                    creator.setAvatarUrl(creator_avatar);
                    creator.setUsername(creator_username);
                    t.setCreator(creator);

//                    details.put("tweet_text",tweet);
//                    details.put("creator_id",Cache.returnUserID(creator_username));
//                    details.put("creator_at",created_at.toString());
//                    details.put("image_url",image_url);
                    //Cache.cacheTweet(id+"",details);


                }
                set.close();

                ValueNode child = nf.pojoNode(t);
                root.set("tweet", child);
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

            } else {
                t.setId(Integer.parseInt(details.get("id")));
                t.setTweetText(details.get("tweet_text"));
                t.setImageUrl(details.get("image_url"));
                t.setCreatedAt(Timestamp.valueOf(details.get("created_at")));
                t.setRetweets(Integer.parseInt(details.get("retweets")));
                t.setFavorites(Integer.parseInt(details.get("favorites")));
//                creator.setName(creator_name);
//                creator.setAvatarUrl(creator_avatar);
//                creator.setUsername(creator_username);
                t.setCreator(creator);
            }
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
