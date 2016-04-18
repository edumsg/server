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

import edumsg.core.*;
import edumsg.redis.Cache;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.node.POJONode;
import org.postgresql.util.PSQLException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.rmi.server.UID;
import java.sql.*;
import java.util.HashMap;
import java.util.logging.Logger;

public class LoginCommand extends Command {
    private final Logger LOGGER = Logger.getLogger(LoginCommand.class.getName());
    private Integer id;
    private String username, name, email, language, country, bio, website, link_color, background_color;
    private Timestamp created_at;
    private String avatar_url;
    private Boolean overlay, protected_tweets;

    @Override
    public void execute() {

        try {
            String sessionID = URLEncoder.encode(new UID().toString(), "UTF-8");
            String cleaned_session = sessionID.replace("%", "\\%");
            dbConn = PostgresConnection.getDataSource().getConnection();
            dbConn.setAutoCommit(false);
            proc = dbConn.prepareCall("{? = call get_password_info(?)}");
            proc.setPoolable(true);
            proc.registerOutParameter(1, Types.VARCHAR);
            proc.setString(2, map.get("username"));
            proc.execute();

            String enc_password = proc.getString(1);

            if (enc_password == null) {
                CommandsHelp.handleError(map.get("app"), map.get("method"),
                        "Invalid username", map.get("correlation_id"), LOGGER);
                return;
            }

            proc.close();
            dbConn.commit();

            boolean authenticated = BCrypt.checkpw(map.get("password"), enc_password);

            if (authenticated) {
                String user_id = Cache.returnUserID(map.get("username"));
                details = null; //Cache.returnUser(user_id);
                User user = new User();
                Statement query = dbConn.createStatement();


                if (details == null) {
                    proc = dbConn.prepareCall("{call login(?,?)}");
                    proc.setPoolable(true);
                    proc.setString(1, map.get("username"));
                    proc.setString(2, cleaned_session);
                    proc.execute();
                    proc.close();
                    dbConn.commit();

                    root.put("app", map.get("app"));
                    root.put("method", map.get("method"));
                    root.put("status", "ok");
                    root.put("code", "200");

                    //new
//                    while (set.next()) {
//                        id = set.getInt("user_id");
//                        username = set.getString("username");
//                        email = set.getString("email");
//                        name = set.getString("name");
//                        language = set.getString("language");
//                        country = set.getString("country");
//                        bio = set.getString("bio");
//                        website = set.getString("website");
//                        created_at = set.getTimestamp("created_at");
//                        avatar_url = set.getString("avatar_url");
//                        overlay = set.getBoolean("overlay");
//                        link_color = set.getString("link_color");
//                        background_color = set.getString("background_color");
//                        protected_tweets = set.getBoolean("protected_tweets");
//
//                        user.setUsername(username);
//                        user.setEmail(email);
//                        user.setName(name);
//                        user.setLanguage(language);
//                        user.setCountry(country);
//                        user.setBio(bio);
//                        user.setWebsite(website);
//                        user.setCreatedAt(created_at);
//                        user.setAvatarUrl(avatar_url);
//                        user.setOverlay(overlay);
//                        user.setLinkColor(link_color);
//                        user.setBackgroundColor(background_color);
//                        user.setProtectedTweets(protected_tweets);
//                        user.setSessionID(sessionID);
//
//                        details = new HashMap<String, String>();
//
//                        details.put("id", id.toString());
//                        details.put("username", username);
//                        details.put("email", email);
//                        details.put("name", name);
//                        details.put("language", language);
//                        details.put("country", country);
//                        details.put("bio", bio);
//                        details.put("website", website);
//                        details.put("created_at", created_at.toString());
//                        details.put("avatar_url", avatar_url);
//                        details.put("overlay", overlay.toString());
//                        details.put("link_color", link_color);
//                        details.put("background_color", background_color);
//                        details.put("protected_tweets", protected_tweets.toString());
//                        details.put("session_id", sessionID);
//
//
//
//                    }
                    user.setSessionID(cleaned_session);
//                    root.put("session_id", sessionID);
                    //Cache.cacheUser(id.toString(), details);

                } else {
                    root.put("app", map.get("app"));
                    root.put("method", map.get("method"));
                    root.put("status", "ok");
                    root.put("code", "200");
                    root.put("session_id", details.get("id"));

                    user.setId(Integer.parseInt(details.get("id")));
                    user.setUsername(details.get("username"));
                    user.setEmail(details.get("email"));
                    user.setName(details.get("name"));
                    user.setLanguage(details.get("language"));
                    user.setCountry(details.get("country"));
                    user.setBio(details.get("bio"));
                    user.setWebsite(details.get("website"));
                    user.setCreatedAt(Timestamp.valueOf(details.get("created_at")));
                    user.setAvatarUrl(details.get("avatar_url"));
                    user.setOverlay(Boolean.parseBoolean(details.get("overlay")));
                    user.setLinkColor(details.get("link_color"));
                    user.setBackgroundColor(details.get("background_color"));
                    user.setProtectedTweets(Boolean.parseBoolean(details.get("protected_tweets")));
                    user.setSessionID(sessionID);
                    //Cache.cacheUserSession(details.get("id"), sessionID);
                }

                POJONode child = nf.POJONode(user);
                root.put("user", child);

                try {
                    CommandsHelp.submit(map.get("app"), mapper.writeValueAsString(root), map.get("correlation_id"), LOGGER);
                } catch (JsonGenerationException e) {
                    //Logger.log(Level.SEVERE, e.getMessage(), e);
                } catch (JsonMappingException e) {
                    //Logger.log(Level.SEVERE, e.getMessage(), e);
                } catch (IOException e) {
                    //Logger.log(Level.SEVERE, e.getMessage(), e);
                }

            } else {
                CommandsHelp.handleError(map.get("app"), map.get("method"), "Invalid Password", map.get("correlation_id"), LOGGER);
            }

        } catch (PSQLException e) {
            CommandsHelp.handleError(map.get("app"), map.get("method"), e.getMessage(), map.get("correlation_id"), LOGGER);
            //Logger.log(Level.SEVERE, e.getMessage(), e);
        } catch (SQLException e) {
            CommandsHelp.handleError(map.get("app"), map.get("method"), e.getMessage(), map.get("correlation_id"), LOGGER);
            //Logger.log(Level.SEVERE, e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            //Logger.log(Level.SEVERE, e.getMessage(), e);
        } catch (Exception e) {
            e.printStackTrace();
            //Logger.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            PostgresConnection.disconnect(set, proc, dbConn, null);
        }
    }
}
