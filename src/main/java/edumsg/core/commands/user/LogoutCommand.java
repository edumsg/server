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
//updated class

package edumsg.core.commands.user;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import edumsg.core.Command;
import edumsg.core.CommandsHelp;
import edumsg.core.PostgresConnection;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;

public class LogoutCommand extends Command implements Runnable {
    private final Logger LOGGER = Logger.getLogger(LogoutCommand.class.getName());
    private static double classVersion = 1.0;

    @Override
    public void execute() {

        //Cache.logoutUser(map.get("user_id"));
        try {
            dbConn = PostgresConnection.getDataSource().getConnection();
            dbConn.setAutoCommit(false);
            proc = dbConn.prepareCall("{call logout(?)}");
            proc.setPoolable(true);
            proc.setString(1, map.get("session_id"));
            proc.execute();

            root.put("app", map.get("app"));
            root.put("method", map.get("method"));
            root.put("status", "ok");
            root.put("code", "200");

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


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static double getClassVersion() {
        return classVersion;
    }
}
