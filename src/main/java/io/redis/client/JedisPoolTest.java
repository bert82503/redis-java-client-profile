package io.redis.client;

import static org.testng.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 类JedisPoolTest.java的实现描述：{@link JedisPool} tutorial
 * 
 * @author huagang.li 2014年11月13日 上午11:45:53
 */
public class JedisPoolTest {

    private JedisPool pool;

    @BeforeClass
    public void init() {
        GenericObjectPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);

        String host = "127.0.0.1";
        int port = 6381;
        int timeout = (int) TimeUnit.MILLISECONDS.toMillis(100L);

        this.pool = new JedisPool(poolConfig, host, port, timeout);
    }

    /**
     * Strings 操作
     */
    @Test
    public void doString() {
        Jedis jedis = this.pool.getResource();

        String key = "hello";
        String value = "world";
        assertEquals(jedis.get(key), null);
        // set
        String statusCode = jedis.set(key, value);
        assertEquals(statusCode, "OK");
        assertEquals(jedis.get(key), "world");
        // delete
        Long delNum = jedis.del(key);
        assertEquals(delNum.longValue(), 1L);
    }

    @AfterClass
    public void destroy() {
        if (this.pool != null) {
            this.pool.close();
        }
    }

}
