/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package io.redis.client;

import static org.testng.Assert.assertEquals;
import io.redis.common.JedisUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.testng.annotations.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.ShardedJedis;

/**
 * Test for {@link ShardedJedis}.
 * 
 * @author huagang.li 2014年12月11日 下午3:19:34
 */
public class ShardedJedisTest {
    
    private static final String LOCALHOST = "localhost";

    /**
     * Test for "Issue - BinaryShardedJedis.disconnect() may occur memory leak".
     * You can find more detailed information at https://github.com/xetorthio/jedis/issues/808
     * 
     * @throws InterruptedException
     */
    @Test
    public void testAvoidLeaksUponDisconnect() throws InterruptedException {
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>(2);
        JedisShardInfo shard1 = new JedisShardInfo(LOCALHOST, Protocol.DEFAULT_PORT);
        // shard1.setPassword("foobared");
        shards.add(shard1);
        JedisShardInfo shard2 = new JedisShardInfo(LOCALHOST, Protocol.DEFAULT_PORT + 1);
        // shard2.setPassword("foobared");
        shards.add(shard2);

        ShardedJedis shardedJedis = new ShardedJedis(shards);
        // establish the connection for two redis servers
        shardedJedis.set("a", "bar");
        JedisShardInfo ak = shardedJedis.getShardInfo("a");
        assertEquals(shard2, ak);
        shardedJedis.set("b", "bar1");
        JedisShardInfo bk = shardedJedis.getShardInfo("b");
        assertEquals(shard1, bk);

        // We set a name to the instance so it's easy to find it
        Iterator<Jedis> it = shardedJedis.getAllShards().iterator();
        Jedis deadClient = it.next();
        // closes a given client connection
        JedisUtils.clientKill(deadClient);
        
        assertEquals(true, deadClient.isConnected());
        assertEquals(false, deadClient.getClient().getSocket().isClosed());
        assertEquals(false, deadClient.getClient().isBroken()); // normal - not found

        // test for original impl
        try {
            shardedJedis.close(); // will call disconnect()
        } catch (Exception e) {
            // ignore exception
        }
        // the two socket connections are all not closed
        assertEquals(true, deadClient.isConnected());
        assertEquals(false, deadClient.getClient().getSocket().isClosed());
        assertEquals(true, deadClient.getClient().isBroken()); // exception - found
        Jedis jedis2 = it.next();
        assertEquals(true, jedis2.isConnected());
        assertEquals(false, jedis2.getClient().getSocket().isClosed());
        assertEquals(false, jedis2.getClient().isBroken());

        // test for new impl
//        shardedJedis.close(); // will call disconnect()
//        // the two socket connections are all closed normally
//        assertEquals(false, deadClient.isConnected());
//        assertEquals(true, deadClient.getClient().getSocket().isClosed());
//        assertEquals(true, deadClient.getClient().isBroken()); // exception - found
//        Jedis jedis2 = it.next();
//        assertEquals(false, jedis2.isConnected());
//        assertEquals(true, jedis2.getClient().getSocket().isClosed());
//        assertEquals(false, jedis2.getClient().isBroken());
    }

}
