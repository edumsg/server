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

package edumsg.redis;

import redis.clients.jedis.Jedis;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class EduMsgRedis {
    public static Jedis redisCache = getConnection();


    private static Jedis getConnection() {
        URI redisURI;
        Jedis jedis = null;
        try {
            redisURI = new URI(System.getenv("REDIS_URL"));
            System.out.println("Redis URI 1 :" + redisURI);
            jedis = new Jedis(redisURI);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch ( NullPointerException e ) {
            System.out.println("Redis URI : local-6379");
            jedis = new Jedis("localhost", 6379);
        }
        return jedis;
    }

    public static void bgSave(){
        Runnable runnable = new Runnable() {
            public void run() {
                String res;
                res = redisCache.bgsave();
                System.out.println(res);
            }
        };
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(runnable, 0, 15, TimeUnit.MINUTES);
    }

    public static void tweetCleaner(){
        Runnable runnable = new Runnable() {
            public void run() {
                String res;
                res = redisCache.bgsave();
                System.out.println(res);
            }
        };
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(runnable, 0, 15, TimeUnit.MINUTES);
    }

    public static void dmCleaner(){
        Runnable runnable = new Runnable() {
            public void run() {
                String res;
                res = redisCache.bgsave();
                System.out.println(res);
            }
        };
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(runnable, 0, 15, TimeUnit.MINUTES);
    }
}
