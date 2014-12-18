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

import io.redis.jedis.CustomShardedJedisPool;
import io.redis.jedis.CustomShardedJedisPoolFactoryBean;
import io.redis.jedis.RedisServiceImpl;
import io.redis.jedis.RedisService;

/**
 * Test for {@link RedisService}.
 * 
 * @author huagang.li 2014年12月15日 下午6:10:01
 */
public class RedisServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(RedisServiceTest.class);

    private RedisService        redisService;

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

        redisService = new RedisServiceImpl();
        CustomShardedJedisPool shardedJedisPool = shardedJedisPoolFactory.getObject();

        RedisServiceImpl redisServiceImpl = (RedisServiceImpl) redisService;
        redisServiceImpl.setShardedJedisPool(shardedJedisPool);
        redisServiceImpl.setEnabled(true);
    }

    private static final String DEFAUL_VALUE = "1";

    private static final String RET_OK       = "OK";

    @Test(enabled = false, description = "验证SET操作")
    public void set() {
        String key = null;

        int size = 7;
        for (int i = 1; i <= size; i++) {
            key = "st_" + i;
            String ret = redisService.set(key, DEFAUL_VALUE);
            assertEquals(ret, RET_OK);

            logger.info("Complete time: {}", Integer.valueOf(i));
        }
    }

    @Test(enabled = false, description = "验证\"自动摘除异常(宕机)的Redis服务器，自动添加恢复正常的Redis服务器\"功能")
    public void autoDetectBrokenRedisServer() throws InterruptedException {
        String key = null;

        int size = 7;
        for (int i = 1; i <= size; i++) {
            key = "st_" + i;
            String ret = redisService.set(key, DEFAUL_VALUE);
            assertEquals(ret, RET_OK);

            logger.info("Complete time: {}", Integer.valueOf(i));
            if (i < size) {
                TimeUnit.SECONDS.sleep(3L);
            }
        }
    }

    @AfterClass(enabled = false)
    public void destroy() {
        ((RedisServiceImpl) redisService).close();
    }

}
