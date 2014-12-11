/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package io.redis.client;

import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
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
	 * 测试"{@link ShardedJedis#disconnect()}的链接资源Socket未关闭"问题。
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void disconnect() throws InterruptedException {
		List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>(2);
		// 6379
		JedisShardInfo shard1 = new JedisShardInfo(LOCALHOST,
				Protocol.DEFAULT_PORT);
		// shard1.setPassword("foobared");
		shards.add(shard1);
		// 6380
		JedisShardInfo shard2 = new JedisShardInfo(LOCALHOST,
				Protocol.DEFAULT_PORT + 1);
		// shard2.setPassword("foobared");
		shards.add(shard2);

		@SuppressWarnings("resource")
		ShardedJedis shardedJedis = new ShardedJedis(shards);
		// establish the connection for two redis servers
		shardedJedis.set("a", "bar");
		JedisShardInfo ak = shardedJedis.getShardInfo("a");
		assertEquals(shard2, ak);
		shardedJedis.set("b", "bar1");
		JedisShardInfo bk = shardedJedis.getShardInfo("b");
		assertEquals(shard1, bk);

		Collection<Jedis> jedisList = shardedJedis.getAllShards();
		Jedis[] jedisArray = jedisList.toArray(new Jedis[jedisList.size()]);
		if (!jedisList.isEmpty()) {
			Jedis jedis1 = jedisArray[0]; // redis1
			String clientList = jedis1.clientList();
			String addr = clientList.split(" ")[1].split("=")[1];
			// kill 'redis1' client connection
			jedis1.clientKill(addr);
			assertEquals(true, jedis1.isConnected());
			assertEquals(false, jedis1.getClient().getSocket().isClosed());
			assertEquals(false, jedis1.getClient().isBroken()); // normal -
																// not found

			// test for original impl
			try {
				shardedJedis.disconnect();
			} catch (Exception e) {
				// ignore exception
			}
			// the two socket connections are all not closed
			assertEquals(true, jedis1.isConnected());
			assertEquals(false, jedis1.getClient().getSocket().isClosed());
			assertEquals(true, jedis1.getClient().isBroken()); // exception
			Jedis jedis2 = jedisArray[1];
			assertEquals(true, jedis2.isConnected());
			assertEquals(false, jedis2.getClient().getSocket().isClosed());
			assertEquals(false, jedis2.getClient().isBroken());

			// test for new impl
			// shardedJedis.disconnect();
			// // the two socket connections are all closed normally
			// assertEquals(false, jedis1.isConnected());
			// assertEquals(true,
			// jedis1.getClient().getSocket().isClosed());
			// // local server
			// assertEquals(true, jedis1.getClient().isBroken());
			// // remote 'Travis CI' for Jedis
			// // assertEquals(false, jedis1.getClient().isBroken());
			// Jedis jedis2 = jedisArray[1];
			// assertEquals(false, jedis2.isConnected());
			// assertEquals(true,
			// jedis2.getClient().getSocket().isClosed());
			// assertEquals(false, jedis2.getClient().isBroken());
		}
	}

}
