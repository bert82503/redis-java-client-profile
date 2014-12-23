/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package io.redis.client;

import static org.testng.Assert.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import io.redis.jedis.impl.CustomShardedJedisPool;
import io.redis.jedis.impl.CustomShardedJedisPoolFactoryBean;
import io.redis.jedis.impl.CustomShardedJedisPoolFactoryBean.PoolBehaviour;
import io.redis.jedis.impl.RedisConfigUtils;
import io.redis.jedis.impl.RedisServiceImpl;
import io.redis.jedis.RedisService;

/**
 * Test for {@link RedisService}.
 * 
 * @author huagang.li 2014年12月15日 下午6:10:01
 */
public class RedisServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(RedisServiceTest.class);

    private RedisService        redisService;

    private String              redisServers;

    @BeforeClass
    public void init() throws Exception {
        redisServers = TestConfigUtils.getRedisServers();

        CustomShardedJedisPoolFactoryBean shardedJedisPoolFactory = new CustomShardedJedisPoolFactoryBean();
        shardedJedisPoolFactory.setRedisServers(redisServers);
        shardedJedisPoolFactory.setTimeoutMillis(100);
        shardedJedisPoolFactory.setMaxTotalNum(32768);
        shardedJedisPoolFactory.setMaxIdleNum(32768);
        shardedJedisPoolFactory.setMinIdleNum(3);
        shardedJedisPoolFactory.setPoolBehaviour(PoolBehaviour.LIFO);
        shardedJedisPoolFactory.setTimeBetweenEvictionRunsSeconds(60);
        shardedJedisPoolFactory.setNumTestsPerEvictionRun(10);
        shardedJedisPoolFactory.setMinEvictableIdleTimeMinutes(30L);
        shardedJedisPoolFactory.setMaxEvictableIdleTimeMinutes(TimeUnit.DAYS.toMinutes(1L));
        shardedJedisPoolFactory.setRemoveAbandonedTimeoutMinutes(5);

        redisService = new RedisServiceImpl();
        CustomShardedJedisPool shardedJedisPool = shardedJedisPoolFactory.getObject();
        redisService.setShardedJedisPool(shardedJedisPool);
        redisService.setEnabled(true);
    }

    private static final int DEFAULT_TIMEOUT = (int) TimeUnit.MILLISECONDS.toMillis(100L);

    @Test(description = "检查每一台Redis服务器是否运行正常")
    public void checkEachRedisServerRunOk() {
        List<JedisShardInfo> shards = RedisConfigUtils.parserRedisServerList(TestConfigUtils.getRedisServers(),
                                                                             DEFAULT_TIMEOUT);
        for (JedisShardInfo shardInfo : shards) {
            // try-with-resources, in Java SE 7 and later
            try (JedisPool pool = new JedisPool(new JedisPoolConfig(), shardInfo.getHost(), shardInfo.getPort(),
                                                shardInfo.getTimeout())) {
                // Jedis implements Closeable. Hence, the jedis instance will be auto-closed after the last statement.
                try (Jedis jedis = pool.getResource()) {
                    String ret = jedis.set("foo", "bar");
                    assertEquals(ret, "OK");
                    String value = jedis.get("foo");
                    assertEquals(value, "bar");
                }
            }
        }
    }

    private static final String RET_OK = "OK";

    @Test(description = "验证 String 的 SET、GET、DEL 命令")
    public void setAndGetAndDel() {
        String ret = redisService.set("foo", "bar");
        assertEquals(ret, RET_OK);

        String value = redisService.get("foo");
        assertEquals(value, "bar");

        long removedKeyNum = redisService.del("foo");
        assertEquals(removedKeyNum, 1L);

        value = redisService.get("foo");
        assertEquals(value, null);
    }

    @Test(description = "验证 String 的 SETEX 命令")
    public void setex() {
        String ret = redisService.setex("str:1", 1, "1");
        assertEquals(ret, RET_OK);

        // 当seconds参数不合法时，返回 null (后台抛出"JedisDataException: ERR invalid expire time in setex")
        ret = redisService.setex("str:0", 0, "0");
        assertEquals(ret, null);

        ret = redisService.setex("str:-1", -1, "-1");
        assertEquals(ret, null);
    }

    @Test(description = "验证 List 的 LPUSH、LRANGE、LTRIM、LLEN、RPOP 命令")
    public void list() {
        // 清空缓存数据
        redisService.ltrim("list", 0, 0);
        redisService.rpop("list");

        // lpush - 左端Push
        long pushedListLength = redisService.lpush("list", "foo");
        assertEquals(pushedListLength, 1L);
        pushedListLength = redisService.lpush("list", "bar");
        assertEquals(pushedListLength, 2L);
        // lrange - 获取所有元素
        List<String> list = redisService.lrange("list", 0, -1);
        assertEquals(list.toString(), "[bar, foo]");
        // 允许重复元素
        pushedListLength = redisService.lpush("list", "foo", "bar");
        assertEquals(pushedListLength, 4L);

        // lrange
        list = redisService.lrange("list", 0, -1);
        assertEquals(list.toString(), "[bar, foo, bar, foo]");
        // 获取若干个元素
        list = redisService.lrange("list", 0, 1);
        assertEquals(list.toString(), "[bar, foo]");

        // ltrim
        // 限制List的长度
        String ret = redisService.ltrim("list", 0, 2);
        assertEquals(ret, RET_OK);
        long len = redisService.llen("list");
        assertEquals(len, 3L);
        list = redisService.lrange("list", 0, -1);
        assertEquals(list.toString(), "[bar, foo, bar]");
        // 截断只剩下表头元素
        ret = redisService.ltrim("list", 0, 0);
        assertEquals(ret, RET_OK);
        list = redisService.lrange("list", 0, -1);
        assertEquals(list.toString(), "[bar]");

        // rpop - 右端Pop
        String value = redisService.rpop("list");
        assertEquals(value, "bar");
        // 列表为空
        len = redisService.llen("list");
        assertEquals(len, 0L);
        list = redisService.lrange("list", 0, -1);
        assertEquals(list.toString(), "[]");
        value = redisService.rpop("list");
        assertEquals(value, null);
    }

    @Test(description = "验证 有序集合(Sorted Set) 的 ZADD、ZRANGEBYSCORE、ZREVRANGEBYSCORE、ZREMRANGEBYSCORE、ZCARD 命令")
    public void sortedSet() {
        // 清空缓存数据
        redisService.zremrangeByScore("zset", Double.MIN_VALUE, Double.MAX_VALUE);

        // zadd
        long newElementNum = redisService.zadd("zset", 3.0, "3.0");
        assertEquals(newElementNum, 1L);
        // 添加"重复元素"失败
        newElementNum = redisService.zadd("zset", 3.0, "3.0");
        assertEquals(newElementNum, 0L);
        newElementNum = redisService.zadd("zset", 1.0, "1.0");
        assertEquals(newElementNum, 1L);
        redisService.zadd("zset", 2.0, "2.0");
        redisService.zadd("zset", 2.0, "2.01");

        // zcard - 获取元素数量
        long zsetElementNum = redisService.zcard("zset");
        assertEquals(zsetElementNum, 4L);
        // zrangeByScore - 按分数正序
        Set<String> zset = redisService.zrangeByScore("zset", Double.MIN_VALUE, Double.MAX_VALUE);
        assertEquals(zset.toString(), "[1.0, 2.0, 2.01, 3.0]");
        zset = redisService.zrangeByScore("zset", Double.MIN_VALUE, Double.MAX_VALUE, 0, 3);
        assertEquals(zset.toString(), "[1.0, 2.0, 2.01]");
        // zrevrangeByScore - 按分数逆序
        zset = redisService.zrevrangeByScore("zset", 2.3, 1);
        assertEquals(zset.toString(), "[2.01, 2.0, 1.0]");
        zset = redisService.zrevrangeByScore("zset", Double.MAX_VALUE, Double.MIN_VALUE, 0, 2);
        assertEquals(zset.toString(), "[3.0, 2.01]");

        // zremrangeByScore - 根据分数排序过滤元素
        long removedElementNum = redisService.zremrangeByScore("zset", 0, 2); // 把"2.01"也过滤掉了，与浮点数在机器的真实表示为准
        assertEquals(removedElementNum, 3L);
        zsetElementNum = redisService.zcard("zset");
        assertEquals(zsetElementNum, 1L);
        zset = redisService.zrangeByScore("zset", Double.MIN_VALUE, Double.MAX_VALUE);
        assertEquals(zset.toString(), "[3.0]");

        // 最大、最小分数参数传反了
        redisService.zrangeByScore("zset", Double.MAX_VALUE, Double.MIN_VALUE);
        redisService.zrevrangeByScore("zset", Double.MIN_VALUE, Double.MAX_VALUE);
    }

    @Test(enabled = false, description = "验证\"自动摘除异常(宕机)的Redis服务器，自动添加恢复正常的Redis服务器\"功能")
    public void autoDetectBrokenRedisServer() throws InterruptedException {
        String key = null;

        int size = 7;
        for (int i = 1; i <= size; i++) {
            key = "st_" + i;
            String ret = redisService.set(key, "1");
            assertEquals(ret, RET_OK);

            logger.info("Complete time: {}", Integer.valueOf(i));
            if (i < size) {
                TimeUnit.SECONDS.sleep(3L);
            }
        }
    }

    @AfterClass
    public void destroy() {
        redisService.close();
    }

}
