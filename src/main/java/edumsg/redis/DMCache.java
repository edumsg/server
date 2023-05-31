package edumsg.redis;

import redis.clients.jedis.Pipeline;

/**
 * Created by ahmed on 5/8/16.
 */
public class DMCache extends Cache {
    //new instance of shared pool to support multithreaded environments
    private Pipeline dmPipeline = jedisCache.pipelined();


}
