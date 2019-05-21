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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edumsg.core.Command;
import edumsg.core.CommandsHelp;
import edumsg.core.PostgresConnection;
import edumsg.redis.Cache;
import edumsg.redis.ListCache;
import edumsg.redis.TweetsCache;
import edumsg.redis.UserCache;
import edumsg.shared.MyObjectMapper;
import org.json.JSONObject;
import org.postgresql.util.PSQLException;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UnRetweetCommand extends Command implements Runnable {
    private final Logger LOGGER = Logger.getLogger(UnRetweetCommand.class.getName());

    @Override
    public void execute() {
        try {
            dbConn = PostgresConnection.getDataSource().getConnection();
            dbConn.setAutoCommit(true);

            proc = dbConn.prepareCall("{? = call unretweet(?,?)}");
            proc.setPoolable(true);

            proc.registerOutParameter(1, Types.INTEGER);
            proc.setInt(2, Integer.parseInt(map.get("tweet_id")));
            proc.setString(3, map.get("session_id"));

            proc.execute();

            int retweets = proc.getInt(1);

            MyObjectMapper mapper = new MyObjectMapper();
            JsonNodeFactory nf = JsonNodeFactory.instance;
            ObjectNode root = nf.objectNode();
            root.put("app", map.get("app"));
            root.put("method", map.get("method"));
            root.put("status", "ok");
            root.put("code", "200");
            root.put("favorites", retweets);

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
