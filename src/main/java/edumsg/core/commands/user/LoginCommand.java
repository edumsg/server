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
import com.fasterxml.jackson.databind.node.ValueNode;
import edumsg.core.*;
import edumsg.redis.UserCache;
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
    private static double classVersion = 1.0;

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
                User user = new User();
                Statement query = dbConn.createStatement();

//                query = dbConn.createStatement();
//                query.setPoolable(true);
//                set = query.executeQuery(String.format("SELECT * FROM login('%s','%s')"
//                        , map.get("username")
//                        , cleaned_session));


                proc = dbConn.prepareCall("{? = call login(?,?)}");
                proc.setPoolable(true);
                proc.registerOutParameter(1, Types.OTHER);
                proc.setString(2, map.get("username"));
                proc.setString(3, cleaned_session);
                proc.execute();
                set = (ResultSet) proc.getObject(1);


                root.put("app", map.get("app"));
                root.put("method", map.get("method"));
                root.put("status", "ok");
                root.put("code", "200");


                while (set.next()) {
                    id = set.getInt("id");
                    username = set.getString("username");
                    email = set.getString("email");
                    name = set.getString("name");
                    language = set.getString("language");
                    country = set.getString("country");
                    bio = set.getString("bio");
                    website = set.getString("website");
                    created_at = set.getTimestamp("created_at");
                    avatar_url = set.getString("avatar_url");
                    overlay = set.getBoolean("overlay");
                    link_color = set.getString("link_color");
                    background_color = set.getString("background_color");
                    protected_tweets = set.getBoolean("protected_tweets");

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
                    user.setSessionID(sessionID);

                    details = new HashMap<String, String>();

                    details.put("id", id.toString());
                    details.put("username", username);
                    details.put("email", email);
                    details.put("name", name);
                    details.put("language", language);
                    details.put("country", country);
                    details.put("bio", bio);
                    details.put("website", website);
                    details.put("created_at", created_at.toString());
                    details.put("avatar_url", avatar_url);
                    details.put("overlay", overlay.toString());
                    details.put("link_color", link_color);
                    details.put("background_color", background_color);
                    details.put("protected_tweets", protected_tweets.toString());
                    details.put("session_id", sessionID);
                }


                proc.close();
                dbConn.commit();

                user.setSessionID(cleaned_session);
                UserCache.cacheUser(id.toString(), details);
                UserCache.mapUsernameID(username, id + "");
                UserCache.cacheUserSession(cleaned_session, details.get("id"));


                ValueNode child = nf.pojoNode(user);
                root.set("user", child);

                try {
                    CommandsHelp.submit(map.get("app"), mapper.writeValueAsString(root), map.get("correlation_id"), LOGGER);
                } catch (Exception e) {
                    e.printStackTrace();
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

    public static double getClassVersion() {
        return classVersion;
    }
}
