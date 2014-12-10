/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package io.redis.client;

import static org.testng.Assert.assertEquals;
import io.redis.jedis.CustomShardedJedisPool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.exceptions.JedisException;

/**
 * Test for {@link CustomShardedJedisPool}.
 * 
 * @author huagang.li 2014年12月6日 下午6:00:00
 */
public class CustomShardedJedisPoolTest {

    private static final Logger    logger          = LoggerFactory.getLogger(CustomShardedJedisPoolTest.class);

    private static final String    DEFAULT_HOST    = "127.0.0.1";

    private static final int       DEFAULT_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(1L);

    private CustomShardedJedisPool pool;

    @BeforeClass
    public void init() throws InterruptedException {
        // used to BTrace
        // TimeUnit.SECONDS.sleep(20L);

        // 获取 twemproxy 服务器列表
        List<HostAndPort> hosts = this.getHostList(ServerFlag.REDIS);

        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>(hosts.size());
        int i = 0;
        for (HostAndPort hostInfo : hosts) {
            String shardName = "SHARD-" + i++;
            JedisShardInfo shardInfo = new JedisShardInfo(hostInfo.getHost(), hostInfo.getPort(), DEFAULT_TIMEOUT,
                                                          shardName);
            shards.add(shardInfo);
        }
        // logger.info("Shard List: " + shards);

        GenericObjectPoolConfig poolConfig = new JedisPoolConfig();
        // 高并发压测
        poolConfig.setMaxTotal(32768);
        poolConfig.setMaxIdle(32768);
        poolConfig.setMinIdle(3);
        // 非阻塞
        poolConfig.setBlockWhenExhausted(false);
        // 阻塞等待一段时间
        // poolConfig.setBlockWhenExhausted(true);
        // poolConfig.setMaxWaitMillis(TimeUnit.MILLISECONDS.toMillis(10L));

        // 在借用池对象时，检测其有效性
        // poolConfig.setTestOnBorrow(true);

        poolConfig.setTimeBetweenEvictionRunsMillis(TimeUnit.SECONDS.toMillis(3L));
        poolConfig.setNumTestsPerEvictionRun(1);
        poolConfig.setMinEvictableIdleTimeMillis(TimeUnit.MINUTES.toMillis(5L)); // 5 minutes
        poolConfig.setSoftMinEvictableIdleTimeMillis(TimeUnit.MINUTES.toMillis(5L)); // 5 minutes

        this.pool = new CustomShardedJedisPool(poolConfig, shards);

        AbandonedConfig abandonedConfig = new AbandonedConfig();
        abandonedConfig.setRemoveAbandonedOnMaintenance(true);
        this.pool.setAbandonedConfig(abandonedConfig);
    }

    private enum ServerFlag {
        REDIS, TWEMPROXY
    }

    private List<HostAndPort> getHostList(ServerFlag serverFlag) {
        int[] ports = null;
        switch (serverFlag) {
            case REDIS:
                // Redis servers
                ports = new int[] { 6379, 6380, 6381 };
                break;
            case TWEMPROXY:
                // Twemproxy servers
                ports = new int[] { 10001 };
                break;
            default:
                throw new IllegalArgumentException("Not has this server flag: " + serverFlag);
        }

        List<HostAndPort> hosts = new ArrayList<HostAndPort>(ports.length);
        for (int port : ports) {
            hosts.add(new HostAndPort(DEFAULT_HOST, port));
        }
        return hosts;
    }

    private static final String DEFAUL_VALUE = "1";

    private static final String RET_OK       = "OK";

    private static final int    SIZE         = 7;

    /**
     * 测试"自动摘除阻塞异常(宕机)的Redis服务器，自动添加恢复正常的Redis服务器"功能。
     * 
     * @throws InterruptedException
     */
    @Test
    public void autoDetectBrokenRedisServer() throws InterruptedException {
        String key = null;

        for (int i = 1; i <= SIZE; i++) {
            ShardedJedis jedis = null;

            key = "st_" + i;
            try {
                // 获取一条Redis连接
                jedis = this.pool.getResource();

                // log Shard info
                JedisShardInfo shardInfo = jedis.getShardInfo(key);
                logger.info("Shard Info: " + shardInfo);

                try {
                    String statusCode = jedis.set(key, DEFAUL_VALUE);
                    assertEquals(statusCode, RET_OK);
                    // 返回连接到连接池
                    jedis.close();
                } catch (JedisException je) {
                    logger.warn("Failed to operate on Jedis Client", je);
                }

                logger.info("Complete time: {}", Integer.valueOf(i));
                if (i < SIZE) {
                    TimeUnit.SECONDS.sleep(5L);
                }

            } catch (Exception e) {
                // failed to borrow or return an object
                if (null != jedis) {
                    jedis = null;
                }
                logger.warn("Failed to borrow or return a pooled object", e);
            }
        }
    }

    @AfterClass
    public void destroy() {
        if (this.pool != null) {
            this.pool.close();
        }
    }

}
