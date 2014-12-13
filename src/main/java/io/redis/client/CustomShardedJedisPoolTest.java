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
        poolConfig.setMinIdle(20);
//         poolConfig.setMinIdle(3); // local test
        // 非阻塞
        poolConfig.setBlockWhenExhausted(false);
        // 阻塞等待一段时间
        // poolConfig.setBlockWhenExhausted(true);
        // poolConfig.setMaxWaitMillis(TimeUnit.MILLISECONDS.toMillis(10L));

        // 关闭"在借用或返回池对象时，检测其有效性"（因为这样对性能影响较大）
        poolConfig.setTestOnBorrow(false);
        poolConfig.setTestOnReturn(false);

        /*
         * "EvictionTimer守护线程"相关配置，用它来检测"空闲对象"
         */
        poolConfig.setTestWhileIdle(true);
        // 每隔5秒钟执行一次，保证异常节点被及时探测到（具体隔多久调度一次，根据业务需求来定）
        poolConfig.setTimeBetweenEvictionRunsMillis(TimeUnit.SECONDS.toMillis(5L));
        // 模拟关闭后台EvictionTimer守护线程
        // poolConfig.setTimeBetweenEvictionRunsMillis(TimeUnit.SECONDS.toMillis(500L));
        // 每次检测10个空闲对象
        poolConfig.setNumTestsPerEvictionRun(10);
        // 当池对象的空闲时间超过该值时，就被纳入到驱逐检测范围
        poolConfig.setSoftMinEvictableIdleTimeMillis(TimeUnit.MINUTES.toMillis(30L));
        // 池的最小驱逐空闲时间(空闲驱逐时间)
        // 当池对象的空闲时间超过该值时，立马被驱逐
        poolConfig.setMinEvictableIdleTimeMillis(TimeUnit.DAYS.toMillis(1L));

        this.pool = new CustomShardedJedisPool(poolConfig, shards);

        // 池对象废弃策略
        AbandonedConfig abandonedConfig = new AbandonedConfig();
        abandonedConfig.setRemoveAbandonedOnMaintenance(true);
        abandonedConfig.setRemoveAbandonedTimeout((int) TimeUnit.MINUTES.toSeconds(5L));
//        abandonedConfig.setRemoveAbandonedTimeout((int) TimeUnit.SECONDS.toSeconds(20L)); // local test
        abandonedConfig.setLogAbandoned(true);
        abandonedConfig.setRemoveAbandonedOnBorrow(false);
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

    private static final int    SIZE         = 17;

    /**
     * 测试"自动摘除阻塞异常(宕机)的Redis服务器，自动添加恢复正常的Redis服务器"功能。
     * 
     * @throws InterruptedException
     */
    @Test
    public void autoDetectBrokenRedisServer() throws InterruptedException {
        ShardedJedis jedis = null;
        JedisShardInfo shardInfo = null;
        String key = null;

        for (int i = 1; i <= SIZE; i++) {
            key = "st_" + i;

            try {
                // 获取一条Redis连接
                jedis = this.pool.getResource();

                // log Shard info
                shardInfo = jedis.getShardInfo(key);
                logger.info("Shard Info: " + shardInfo);

                String statusCode = jedis.set(key, DEFAUL_VALUE);
                assertEquals(statusCode, RET_OK);
                // 返回连接到连接池
                jedis.close();
                
//                // 关闭第一个处理节点(6380)的服务端连接，但第二次请求又会重新连接上
//                if (1 == i) {
//                    JedisUtils.clientKill(jedis.getShard(key));
//                }
            } catch (JedisException je) {
                String errorMsg = String.format("Failed to operate on '%s' Jedis Client", shardInfo);
                logger.warn(errorMsg, je);
            }

            logger.info("Complete time: {}", Integer.valueOf(i));
            if (i < SIZE) {
                TimeUnit.SECONDS.sleep(3L);
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
