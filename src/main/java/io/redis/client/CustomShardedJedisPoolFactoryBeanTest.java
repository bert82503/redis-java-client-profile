/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package io.redis.client;

import static org.testng.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.exceptions.JedisException;
import io.redis.jedis.CustomShardedJedisPool;
import io.redis.jedis.CustomShardedJedisPoolFactoryBean;

/**
 * Test for {@link CustomShardedJedisPoolFactoryBean}.
 * 
 * @author huagang.li 2014年12月13日 下午3:22:36
 */
public class CustomShardedJedisPoolFactoryBeanTest {

    private static final Logger    logger = LoggerFactory.getLogger(CustomShardedJedisPoolFactoryBeanTest.class);

    private CustomShardedJedisPool pool;

    @BeforeClass(enabled = false)
    public void init() throws Exception {
        CustomShardedJedisPoolFactoryBean shardedJedisPoolFactory = new CustomShardedJedisPoolFactoryBean();
        shardedJedisPoolFactory.setRedisServers("192.168.6.189:6379:Shard-01,192.168.6.189:6380:Shard-02,192.168.6.189:6381:Shard-03");
        shardedJedisPoolFactory.setTimeoutMillis(100);
        shardedJedisPoolFactory.setMaxTotalNum(32768);
        shardedJedisPoolFactory.setMaxIdleNum(32768);
        shardedJedisPoolFactory.setMinIdleNum(3);
        shardedJedisPoolFactory.setMinEvictableIdleTimeMinutes(30L);
        shardedJedisPoolFactory.setMaxEvictableIdleTimeMinutes(TimeUnit.DAYS.toMinutes(1L));
        shardedJedisPoolFactory.setRemoveAbandonedTimeoutMinutes(5);

        pool = shardedJedisPoolFactory.getObject();
    }

    private static final String DEFAUL_VALUE = "1";

    private static final String RET_OK       = "OK";

    @Test(enabled = false, description = "验证SET操作")
    public void set() {
        ShardedJedis jedis = null;
        JedisShardInfo shardInfo = null;
        String key = null;

        int size = 5;
        for (int i = 1; i <= size; i++) {
            key = "st_" + i;

            try {
                // 获取一条Jedis连接
                jedis = this.pool.getResource();

                // log Shard info
                shardInfo = jedis.getShardInfo(key);
                logger.info("Shard Info: " + shardInfo);

                String statusCode = jedis.set(key, DEFAUL_VALUE);
                assertEquals(statusCode, RET_OK);

                // 返回Jedis连接到连接池
                jedis.close();
            } catch (JedisException je) {
                String errorMsg = String.format("Failed to operate on '%s' Jedis Client", shardInfo);
                logger.warn(errorMsg, je);
            }

            logger.info("Complete time: {}", Integer.valueOf(i));
        }
    }

    @AfterClass(enabled = false)
    public void destroy() {
        pool.close();
    }

}
