package io.redis.client;

import static java.lang.System.out;
import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

/**
 * 类ShardedJedisPoolTest.java的实现描述：{@link ShardedJedisPool} tutorial
 * 
 * @author huagang.li 2014年11月13日 下午4:06:19
 */
public class ShardedJedisPoolTest {

    private static final int DEFAULT_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(1L);

    private ShardedJedisPool pool;

    @BeforeClass
    public void init() {
        List<HostInfo> hosts = this.getHostList();

        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>(hosts.size());
        for (HostInfo hostInfo : hosts) {
            JedisShardInfo shardInfo = new JedisShardInfo(hostInfo.getHost(), hostInfo.getPort(), DEFAULT_TIMEOUT);
            shards.add(shardInfo);
        }

        GenericObjectPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMinIdle(1);
        // poolConfig.setMaxWaitMillis(TimeUnit.SECONDS.toMillis(1L));
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRunsMillis(TimeUnit.MILLISECONDS.toMillis(500L));
        // poolConfig.setMinEvictableIdleTimeMillis(TimeUnit.MILLISECONDS.toMillis(500L));
        // poolConfig.setSoftMinEvictableIdleTimeMillis(TimeUnit.SECONDS.toMillis(1L));

        this.pool = new ShardedJedisPool(poolConfig, shards);
    }

    private List<HostInfo> getHostList() {
        List<HostInfo> hosts = new ArrayList<HostInfo>(3);
        String host = "127.0.0.1";
        hosts.add(new HostInfo(host, 6379));
        hosts.add(new HostInfo(host, 6380));
        hosts.add(new HostInfo(host, 6381));
        return hosts;
    }

    private class HostInfo {

        private String host;
        private int    port;

        public HostInfo(String host, int port){
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

    }

    @Test
    public void doString() throws InterruptedException {
        for (int i = 0; i < 1; i++) {
            TimeUnit.SECONDS.sleep(3L);
            // set
            ShardedJedis jedis = this.pool.getResource();
            String statusCode = jedis.set("1", "1");
            assertEquals(statusCode, "OK");
            this.pool.returnResource(jedis);
            out.println("Complete: 1");

            TimeUnit.SECONDS.sleep(3L);
            // set
            ShardedJedis jedis2 = this.pool.getResource();
            statusCode = jedis2.set("2", "2");
            assertEquals(statusCode, "OK");
            this.pool.returnResource(jedis2);
            out.println("Complete: 2");

            TimeUnit.SECONDS.sleep(3L);
            // set
            ShardedJedis jedis3 = this.pool.getResource();
            statusCode = jedis3.set("3", "3");
            assertEquals(statusCode, "OK");
            this.pool.returnResource(jedis3);
            out.println("Complete: 3");

            out.println("Complete time: " + i);
        }
    }

    @AfterClass
    public void destroy() {
        if (this.pool != null) {
            this.pool.close();
        }
    }

}
