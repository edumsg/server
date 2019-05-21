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
import edumsg.core.Command;
import edumsg.core.CommandsHelp;
import edumsg.core.PostgresConnection;
import edumsg.redis.Cache;
import edumsg.redis.ListCache;
import edumsg.redis.UserCache;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UnFollowCommand extends Command implements Runnable {
    private final Logger LOGGER = Logger.getLogger(UnFollowCommand.class.getName());

    @Override
    public void execute() {

        try {
            dbConn = PostgresConnection.getDataSource().getConnection();
            dbConn.setAutoCommit(true);

            proc = dbConn.prepareCall("{call unfollow(?,?)}");
            proc.setPoolable(true);

            proc.setString(1, map.get("session_id"));
            proc.setInt(2, Integer.parseInt(map.get("followee_id")));

            proc.execute();

            proc.close();

            root.put("app", map.get("app"));
            root.put("method", map.get("method"));
            root.put("status", "ok");
            root.put("code", "200");

            CommandsHelp.submit(map.get("app"), mapper.writeValueAsString(root), map.get("correlation_id"), LOGGER);

            String sessionID = map.get("session_id");

            String [] types = {"rt","nw","va","dbt"};
            int i = 0;

            while (i < types.length) {

                String userTweetsCacheEntry = UserCache.userCache.get("user_tweets_" + types[i] + ":" + sessionID);
                String timelineCacheEntry = UserCache.userCache.get("timeline_" + types[i] + ":" + sessionID);

                CommandsHelp.invalidateCacheEntry(UserCache.userCache,userTweetsCacheEntry,"user_tweets_",sessionID,types[i]);
                CommandsHelp.invalidateCacheEntry(UserCache.userCache,timelineCacheEntry,"timeline_",sessionID,types[i]);

                i++;
            }

            String followersCacheEntry = UserCache.userCache.get("followers:" + sessionID);
            String followingCacheEntry = UserCache.userCache.get("following:" + sessionID);
            String getListFeedsCacheEntry =   ListCache.listCache.get("get_list_feeds:" + sessionID);

            CommandsHelp.invalidateCacheEntry(UserCache.userCache,followersCacheEntry,"followers",sessionID);
            CommandsHelp.invalidateCacheEntry(UserCache.userCache,followingCacheEntry,"following",sessionID);
            CommandsHelp.invalidateCacheEntry(UserCache.userCache,getListFeedsCacheEntry,"get_list_feeds",sessionID);

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
