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
import edumsg.core.*;
import edumsg.redis.Cache;
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

public class GetUserTweetsCommand extends Command implements Runnable {
    private final Logger LOGGER = Logger.getLogger(GetUserTweetsCommand.class.getName());

    @Override
    public void execute() {

        try {
            dbConn = PostgresConnection.getDataSource().getConnection();
            dbConn.setAutoCommit(false);

            proc = dbConn.prepareCall("{? = call get_tweets(?,?)}");
            proc.setPoolable(true);

            proc.registerOutParameter(1, Types.OTHER);
            proc.setString(2, map.get("session_id"));
            proc.setString(3,map.get("type"));

            proc.execute();
            set = (ResultSet) proc.getObject(1);

            ArrayNode tweets = nf.arrayNode();

            root.put("app", map.get("app"));
            root.put("method", map.get("method"));
            root.put("status", "ok");
            root.put("code", "200");

            while (set.next()) {

                Integer id = set.getInt("id");
                String tweet = set.getString("tweet_text");
                String image_url = set.getString("image_url");
                Timestamp created_at = set.getTimestamp("creation");
                Integer creator_id = set.getInt("creator_id");
                String creator_name = set.getString("name");
                String creator_username = set.getString("username");
                String creator_avatar = set.getString("avatar_url");

                Boolean is_retweeted = set.getBoolean("is_retweeted");
                Boolean is_favorited = set.getBoolean("is_favorited");
                Boolean has_replies = set.getBoolean("has_replies");

                Tweet t = new Tweet();
                t.setId(id);
                t.setTweetText(tweet);
                t.setImageUrl(image_url);
                t.setCreatedAt(created_at);
//                t.setIsRetweeted(is_retweeted);
//                t.setIsFavorited(is_favorited);
//                t.setHasReplies(has_replies);

                User creator = new User();
                creator.setId(creator_id);
                creator.setName(creator_name);
                creator.setAvatarUrl(creator_avatar);
                creator.setUsername(creator_username);

                t.setCreator(creator);
                tweets.addPOJO(t);
                }

            set.close();
            proc.close();

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
