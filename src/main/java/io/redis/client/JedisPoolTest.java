package io.redis.client;

import static org.testng.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(JedisPoolTest.class);

    private JedisPool           pool;

    @BeforeClass
    public void init() {
        GenericObjectPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(1);
        poolConfig.setBlockWhenExhausted(false);
        poolConfig.setMaxWaitMillis(TimeUnit.MILLISECONDS.toMillis(30L));
        poolConfig.setTestOnBorrow(true);

        String host = "127.0.0.1";
        int port = 10001;
        int timeout = (int) TimeUnit.MILLISECONDS.toMillis(100L);

        this.pool = new JedisPool(poolConfig, host, port, timeout);
    }

    /**
     * 测试 Strings 操作。
     */
    @Test
    public void doString() {
        Jedis jedis = this.pool.getResource();

        String key = "hello";
        String value = "world";

        // get
        assertEquals(jedis.get(key), null);
        // set
        String statusCode = jedis.set(key, value);
        assertEquals(statusCode, "OK");
        // get
        assertEquals(jedis.get(key), "world");
        // delete
        Long delNum = jedis.del(key);
        assertEquals(delNum.longValue(), 1L);
    }

    /**
     * 验证连接池功能。
     * <p>
     * 
     * <pre>
     * 通过"sudo netstat -anp | grep nut"定期查看程序运行时，仅有如下一条类似信息(10001是nutcracker的端口号)：
     *      tcp        0      0 127.0.0.1:10001         127.0.0.1:43446         ESTABLISHED 17636/nutcracker
     * </pre>
     * 
     * <font color="red">测试发现，一次命令操作都会触发nutcracker与后端redis服务器重新建立一条新的链接！</font>
     * 
     * @throws InterruptedException
     */
    @Test
    public void verifyConnectionPoolFeature() throws InterruptedException {
        Jedis jedis = this.pool.getResource();

        String key = "pool";
        String value = "yes";

        for (int i = 0; i < 3; i++) {
            // set
            String statusCode = jedis.set(key, value);
            assertEquals(statusCode, "OK");

            // get
            // String v = jedis.get(key);
            // assertEquals(v, "yes");

            // delete
            Long delNum = jedis.del(key);
            assertEquals(delNum.longValue(), 1L);

            if (logger.isInfoEnabled()) {
                logger.info("Complete time: {}", Integer.valueOf(i));
            }

            TimeUnit.SECONDS.sleep(10L);
        }

        this.pool.returnResource(jedis);
    }

    @AfterClass
    public void destroy() {
        if (this.pool != null) {
            this.pool.close();
        }
    }

}
