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

package edumsg.core.commands.list;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import edumsg.core.*;
import edumsg.redis.Cache;
import edumsg.redis.ListCache;
import org.json.JSONObject;
import org.postgresql.util.PSQLException;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GetListFeedsCommand extends Command implements Runnable {
    private final Logger LOGGER = Logger.getLogger(GetListFeedsCommand.class.getName());
    private static double classVersion = 1.0;

    @Override
    public void execute() {

        ResultSet set = null;
        try {
            dbConn = PostgresConnection.getDataSource().getConnection();
            dbConn.setAutoCommit(false);
            proc = dbConn.prepareCall("{? = call get_list_feeds(?)}");
            proc.setPoolable(true);
            proc.registerOutParameter(1, Types.OTHER);
            proc.setInt(2, Integer.parseInt(map.get("list_id")));
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
                String creator_name = set.getString(5);
                String creator_username = set.getString(6);
                String creator_avatar = set.getString(7);
                String retweeter = set.getString(8);
                Integer creator_id = set.getInt(9);
                Integer retweeter_id = set.getInt(10);

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
                if (!creator_name.equals(retweeter)) {
                    User r = new User();
                    r.setId(retweeter_id);
                    r.setName(retweeter);
                    t.setRetweeter(r);
                }

                tweets.addPOJO(t);
            }

            root.set("list_feeds", tweets);
            try {
                CommandsHelp.submit(map.get("app"), mapper.writeValueAsString(root), map.get("correlation_id"), LOGGER);
                JSONObject cacheEntry = new JSONObject(mapper.writeValueAsString(root));
                cacheEntry.put("cacheStatus", "valid");
                ListCache.listCache.set("get_list_feeds:" + map.getOrDefault("session_id", ""), cacheEntry.toString());
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
            PostgresConnection.disconnect(set, proc, dbConn);
        }
    }

    public static double getClassVersion() {
        return classVersion;
    }
}
