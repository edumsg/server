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

package edumsg.core;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edumsg.activemq.ActiveMQConfig;
import edumsg.activemq.Producer;
import edumsg.redis.UserCache;
import edumsg.shared.MyObjectMapper;
import org.json.JSONObject;
import org.postgresql.util.PSQLException;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;

public class CommandsHelp {

    public static void handleError(String app, String method, String errorMsg,
                                   String correlationID, Logger logger) {
        JsonNodeFactory nf = JsonNodeFactory.instance;
        MyObjectMapper mapper = new MyObjectMapper();
        ObjectNode node = nf.objectNode();
        node.put("app", app);
        node.put("method", method);
        node.put("status", "Bad Request");
        node.put("code", "400");
        node.put("message", errorMsg);
        try {
            submit(app, mapper.writeValueAsString(node), correlationID, logger);
        } catch (JsonGenerationException e) {
            //logger.log(Level.SEVERE, e.getMessage(), e);
        } catch (JsonMappingException e) {
            //logger.log(Level.SEVERE, e.getMessage(), e);
        } catch (IOException e) {
            //logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public static void submit(String app, String json, String correlationID,
                              Logger logger) {
        System.out.println("Commands Help Class :: JSON: " + json);
        Producer p = new Producer(new ActiveMQConfig(app.toUpperCase()
                + ".OUTQUEUE"));
        p.send(json, correlationID, logger);
    }

    public static String getErrorMessage(String app, String method, Throwable t) {
        // Default Error Message response
        String errMsg = t.getMessage();

        // Frequently Occurring Error Messages
        String valueTooLong = "value too long";
        String uniqueConstraint = "unique constraint";

        if (t instanceof PSQLException) {
            if ( app.equals("dm") ) {
                if (errMsg.contains(valueTooLong)) return "DM length cannot exceed 140 character";
                switch (method) {
                    case "create_conversation":
                        if (errMsg.contains("following you")) return "User must be following you first";

                    case "delete_conversation":
                        if (errMsg.contains("cannot delete")) return "You are not part of this conversation";

                    default: return errMsg;
                }
            }

            if ( app.equals("list") ) {
                if (errMsg.contains("no such list exists")) return " This list is no longer available";
                switch (method) {
                    case "add_member":
                        if (errMsg.contains(uniqueConstraint)) return "Membership already exists";

                    case "update_list":
                    case "subscribe":
                    case "get_list":
                    case "create_list_with_members":
                    case "create_list":
                        if (errMsg.contains(uniqueConstraint) && errMsg.contains("(name)") ) return "List name already exists";
                        if (errMsg.contains(valueTooLong)) return "List name is too long";

                    case "delete_conversation":
                        if (errMsg.contains("cannot delete")) return "You can only delete your own lists";

                    case "unsubscribe":
                        if (errMsg.contains("cannot unsubscribe")) return "You cannot unsubscribe from your own list";

                    default: return errMsg;
                }
            }

            if ( app.equals("tweet") ) {
                switch (method) {
                    case "delete_tweet":
                        if (errMsg.contains("only the tweet's owner")) return "Only the tweet's owner can delete this tweet";

                    case "favorite":
                        if (errMsg.contains(uniqueConstraint)) return "You already favourite this tweet";

                    case "reply":
                    case"tweet":
                        if (errMsg.contains(valueTooLong)) return "Tweet exceeds 140 character";

                    case "report_tweet":
                        if (errMsg.contains(uniqueConstraint)) return "You already reported this tweet";

                    case "retweet":
                        if (errMsg.contains(uniqueConstraint)) return "You already retweeted this tweet";

                    default: return errMsg;
                }
            }

            if ( app.equals("user") ) {
                switch (method) {
                    case "update_user":
                    case "register":
                        if (errMsg.contains(uniqueConstraint)) {
                            if (errMsg.contains("(username)")) return "Username already exists";
                            if (errMsg.contains("(email)")) return "Email already exists";
                        }
                    case "report_user":
                        if (errMsg.contains(uniqueConstraint)) return "You already reported this user";


                    default: return errMsg;
                }
            }

        }

        if(t instanceof SQLException) {
            if( app.equals("list") ) {
                switch (method) {
                    case "get_list":
                    case "create_list": return "List name already exists";
                    default: return  errMsg;
                }
            }
        }

        return errMsg;
    }

    public static void invalidateCacheEntry (Jedis cache, String cachedEntry, String columnName, String sessionID) {
        if (cachedEntry != null) {
            JSONObject cacheEntryJson = new JSONObject(cachedEntry);
            cacheEntryJson.put("cacheStatus", "invalid");
            cache.set(columnName + ":" + sessionID, cacheEntryJson.toString());
        }
    }

    public static void validateCacheEntry (Jedis cache, String cachedEntry, String columnName, String sessionID) {
        if (cachedEntry != null) {
            JSONObject cacheEntryJson = new JSONObject(cachedEntry);
            cacheEntryJson.put("cacheStatus", "valid");
            cache.set(columnName + ":" + sessionID, cacheEntryJson.toString());
        }
    }

    public static void invalidateCacheEntry (Jedis cache, String cachedEntry, String columnName, String sessionID, String type) {
        if (cachedEntry != null) {
            JSONObject cacheEntryJson = new JSONObject(cachedEntry);
            cacheEntryJson.put("cacheStatus", "invalid");
            cache.set(columnName + type + ":" + sessionID, cacheEntryJson.toString());
        }
    }

    public static void validateCacheEntry (Jedis cache, String cachedEntry, String columnName, String sessionID, String type) {
        if (cachedEntry != null) {
            JSONObject cacheEntryJson = new JSONObject(cachedEntry);
            cacheEntryJson.put("cacheStatus", "valid");
            cache.set(columnName + type + ":" + sessionID, cacheEntryJson.toString());
        }
    }
}
