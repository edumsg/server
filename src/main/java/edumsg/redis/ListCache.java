package edumsg.redis;

import redis.clients.jedis.Pipeline;

import java.util.Map;

public class ListCache extends Cache {

    private Pipeline listPipeline = jedisCache.pipelined();

    public static void deleteList(String list_id) {

    }

    public void createList(String id, Map<String, String> members) {
        if (!Cache.checkNulls(members)) {
            jedisCache.hmset("list:" + id, members);
        }
    }

    public void addMemberList(String list_id, String member_id) {
        if ((list_id != null) && (member_id != null)) {
            jedisCache.sadd("listmember:" + list_id, member_id);
        }
    }

    public boolean listExists(String id) {
        return jedisCache.exists("lists:" + id);
    }

}
