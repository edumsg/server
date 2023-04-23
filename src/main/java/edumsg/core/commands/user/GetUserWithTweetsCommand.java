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

package edumsg.core.commands.user;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import edumsg.core.*;
import org.postgresql.util.PSQLException;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.logging.Logger;

public class GetUserWithTweetsCommand extends Command implements Runnable {
    private static double classVersion = 1.0;
    private final Logger LOGGER = Logger.getLogger(GetUserCommand.class.getName());

    public static double getClassVersion() {
        return classVersion;
    }

    @Override
    public void execute() {

        try {
            details = null; //Cache.returnUser(map.get("username"));
            User user = new User();


            dbConn = PostgresConnection.getDataSource().getConnection();
            dbConn.setAutoCommit(false);
            proc = dbConn.prepareCall("{? = call get_user_with_tweets(?)}");
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
                String username = set.getString(2);
                String email = set.getString(3);
                String name = set.getString(5);
                String language = set.getString(6);
                String country = set.getString(7);
                String bio = set.getString(8);
                String website = set.getString(9);
                Timestamp created_at = set.getTimestamp(10);
                String avatar_url = set.getString(11);
                Boolean overlay = set.getBoolean(12);
                String link_color = set.getString(13);
                String background_color = set.getString(14);
                Boolean protected_tweets = set.getBoolean(15);

                user.setId(id);
                user.setUsername(username);
                user.setEmail(email);
                user.setName(name);
                user.setLanguage(language);
                user.setCountry(country);
                user.setBio(bio);
                user.setWebsite(website);
                user.setCreatedAt(created_at);
                user.setAvatarUrl(avatar_url);
                user.setOverlay(overlay);
                user.setLinkColor(link_color);
                user.setBackgroundColor(background_color);
                user.setProtectedTweets(protected_tweets);


                Integer tweet_id = set.getInt(16);
                String tweet = set.getString(17);
                Timestamp tweet_created_at = set.getTimestamp(19);
                String image_url = set.getString(20);


                Tweet t = new Tweet();
                t.setId(tweet_id);
                t.setTweetText(tweet);
                t.setImageUrl(image_url);
                t.setCreatedAt(tweet_created_at);
                t.setCreator(user);

                tweets.addPOJO(t);

                System.out.println(tweet);

            }


            ValueNode child = nf.pojoNode(user);
            root.set("user", child);
//            root.set("tweets", tweets);


            set.close();
            proc.close();
            root.set("tweets", tweets);


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
            PostgresConnection.disconnect(set, proc, dbConn, null);
        }
    }
}
