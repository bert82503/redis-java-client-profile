package io.redis.client;

import static java.lang.System.out;
import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import redis.clients.jedis.HostAndPort;
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

    private static final String DEFAULT_HOST    = "127.0.0.1";

    private static final int    DEFAULT_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(1L);

    private ShardedJedisPool    pool;

    @BeforeClass
    public void init() {
        // 获取 twemproxy 服务器列表
        List<HostAndPort> hosts = this.getHostList(ServerFlag.TWEMPROXY);

        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>(hosts.size());
        for (HostAndPort hostInfo : hosts) {
            JedisShardInfo shardInfo = new JedisShardInfo(hostInfo.getHost(), hostInfo.getPort(), DEFAULT_TIMEOUT);
            shards.add(shardInfo);
        }

        GenericObjectPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(3);
        poolConfig.setMaxWaitMillis(TimeUnit.SECONDS.toMillis(1L));

        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestWhileIdle(true);

        this.pool = new ShardedJedisPool(poolConfig, shards);
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

    private static final String TWEMPROXY_FILE = "twemproxy.txt";

    private static final int    SIZE           = 1001;

    /**
     * 测试 Twemproxy 的分布式 Sharding 功能。
     * 
     * @throws IOException
     */
    @Test
    public void sharding() {
        ShardedJedis jedis = this.pool.getResource();

        PrintWriter writer = null;

        try {
            writer = new PrintWriter(new FileWriter(TWEMPROXY_FILE));

            // 样本数量
            Random rand = new Random(System.currentTimeMillis());
            for (int i = 1; i < SIZE; i++) {
                int v = rand.nextInt();
                String key = Integer.toString(v);
                writer.println(key);
                // SET
                jedis.set(key, "1");
            }
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * 验证 Twemproxy 的分布式 Sharding 功能。
     * 
     * @throws IOException
     */
    @Test
    public void checkSharding() throws IOException {
        // 读取待测试 Key
        File file = new File(TWEMPROXY_FILE);
        List<String> keys = FileUtils.readLines(file);

        /*
         * 1. 获取 redis 服务器列表；
         * 2. 对每台 redis 服务器循环进行 Key GET 操作，获取命中数。
         */
        List<HostAndPort> hostList = this.getHostList(ServerFlag.REDIS);
        for (HostAndPort hostInfo : hostList) {
            List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>(1);
            JedisShardInfo shardInfo = new JedisShardInfo(hostInfo.getHost(), hostInfo.getPort(), DEFAULT_TIMEOUT);
            shards.add(shardInfo);

            GenericObjectPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(10);
            poolConfig.setMaxIdle(10);
            poolConfig.setMinIdle(3);
            poolConfig.setMaxWaitMillis(TimeUnit.SECONDS.toMillis(1L));
            poolConfig.setTestOnBorrow(true);

            ShardedJedisPool jedisPool = new ShardedJedisPool(poolConfig, shards);
            try {
                ShardedJedis jedis = jedisPool.getResource();

                // 统计命中率
                int hits = 0;
                for (String key : keys) {
                    String value = jedis.get(key);
                    if (!StringUtils.isEmpty(value)) {
                        ++hits;
                        // 清理缓存数据
                        jedis.del(key);
                    }
                }

                out.println("Host " + hostInfo + "'s hit ratio: " + hits);
            } finally {
                jedisPool.close();
            }

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
