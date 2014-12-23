package io.redis.client;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.exceptions.JedisException;

/**
 * Test for {@link ShardedJedisPool}.
 * 
 * @author huagang.li 2014年11月13日 下午4:06:19
 */
public class ShardedJedisPoolTest {

	private static final Logger logger = LoggerFactory
			.getLogger(ShardedJedisPoolTest.class);

	private static final String DEFAULT_HOST = "127.0.0.1";

	private static final int DEFAULT_TIMEOUT = (int) TimeUnit.SECONDS
			.toMillis(1L);

	private ShardedJedisPool pool;

	@BeforeClass
	public void init() {
		// 获取 twemproxy 服务器列表
		List<HostAndPort> hosts = this.getHostList(ServerFlag.REDIS);

		List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>(
				hosts.size());
		for (HostAndPort hostInfo : hosts) {
			JedisShardInfo shardInfo = new JedisShardInfo(hostInfo.getHost(),
					hostInfo.getPort(), DEFAULT_TIMEOUT);
			shards.add(shardInfo);
		}

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

		this.pool = new ShardedJedisPool(poolConfig, shards);
	}

	private enum ServerFlag {
		REDIS, TWEMPROXY
	}

	private List<HostAndPort> getHostList(ServerFlag serverFlag) {
		int[] ports = null;
		switch (serverFlag) {
			case REDIS :
				// Redis servers
				ports = new int[]{6379, 6380, 6381};
				break;
			case TWEMPROXY :
				// Twemproxy servers
				ports = new int[]{10001};
				break;
			default :
				throw new IllegalArgumentException("Not has this server flag: "
						+ serverFlag);
		}

		List<HostAndPort> hosts = new ArrayList<HostAndPort>(ports.length);
		for (int port : ports) {
			hosts.add(new HostAndPort(DEFAULT_HOST, port));
		}
		return hosts;
	}

	private static final String TWEMPROXY_FILE = "twemproxy.txt";

	private static final String DEFAUL_VALUE = "1";

	private static final String RET_OK = "OK";

	private static final int SIZE = 7;

	private AtomicInteger counter = new AtomicInteger(1);

	@Test
	public void serverDown() throws InterruptedException {
		String key = null;

		for (int i = 1; i <= SIZE; i++) {
			ShardedJedis jedis = null;

			key = "st_" + i;
			try {
				// 获取一条Redis连接
				jedis = this.pool.getResource();
				JedisShardInfo shardInfo = jedis.getShardInfo(key);
				logger.info("Shard Info: " + shardInfo);

				try {
					String statusCode = jedis.set(key, DEFAUL_VALUE);
					assertEquals(statusCode, RET_OK);
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
				logger.warn("Failed to borrow or return a pooled object", e);
			}
		}
	}

	/**
	 * 高并发压力测试。
	 * <p>
	 * 因为Twemproxy与后端redis服务器在每执行一条命令时，都会创建一条新的链接。
	 */
	@Test(invocationCount = 20000, threadPoolSize = 1024)
	public void highConcurrencyStressTest() {
		long start = System.currentTimeMillis();

		// 获取一条Redis连接
		ShardedJedis jedis = this.pool.getResource();
		// SET
		String key = "st_" + counter.getAndIncrement();
		String statusCode = jedis.set(key, DEFAUL_VALUE);
		assertEquals(statusCode, RET_OK);
		// 返回连接到连接池
		jedis.close();

		long runTime = System.currentTimeMillis() - start;
		if (runTime > 10) {
			logger.info("{}'s run time: {}", key, runTime);
		}
	}

	/**
	 * 测试 Twemproxy 的
	 * "Shard data automatically across multiple servers (跨多台服务器自动切分数据)" 功能。
	 * <p>
	 * <font color="red">注意：记得调整随机样本数量({@link #SIZE})的值！建议该值被设置为大于1000</font>
	 * 
	 * @throws IOException
	 */
	@Test
	public void sharding() {
		PrintWriter writer = null;

		try {
			writer = new PrintWriter(new FileWriter(TWEMPROXY_FILE));

			ShardedJedis jedis = this.pool.getResource();
			// 样本数量
			Random rand = new Random();
			for (int i = 0; i < SIZE; i++) {
				int v = rand.nextInt();
				String key = Integer.toString(v);
				writer.println(key);
				// SET
				jedis.set(key, DEFAUL_VALUE);
			}
			jedis.close();
		} catch (IOException e) {
			System.err.println(e);
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}

	/**
	 * 验证 Twemproxy 的"跨多台服务器自动切分数据"功能。
	 * <p>
	 * 通过统计随机样本 Keys 的"命中率"，分布在每台服务器的Key的总数差不多，分布还算均匀。
	 * 
	 * @throws IOException
	 */
	@Test
	public void checkSharding() throws IOException {
		// 读取待测试 Key
		File file = new File(TWEMPROXY_FILE);
		List<String> keys = FileUtils.readLines(file);

		/*
		 * 1. 获取 redis 服务器列表； 2. 对每台 redis 服务器循环进行 Key GET 操作，获取命中数。
		 */
		List<HostAndPort> hostList = this.getHostList(ServerFlag.REDIS);
		for (HostAndPort hostInfo : hostList) {
			List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>(1);
			JedisShardInfo shardInfo = new JedisShardInfo(hostInfo.getHost(),
					hostInfo.getPort(), DEFAULT_TIMEOUT);
			shards.add(shardInfo);

			GenericObjectPoolConfig poolConfig = new JedisPoolConfig();
			poolConfig.setMaxTotal(10);
			poolConfig.setMaxIdle(10);
			poolConfig.setMinIdle(3);
			poolConfig.setMaxWaitMillis(TimeUnit.SECONDS.toMillis(1L));
			poolConfig.setTestOnBorrow(true);

			ShardedJedisPool jedisPool = new ShardedJedisPool(poolConfig,
					shards);
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

				if (logger.isInfoEnabled()) {
					logger.info("Host {}'s hit ratio: {}", hostInfo, hits);
				}
			} finally {
				jedisPool.close();
			}

		}
	}

	/**
	 * 验证 Twemproxy 的 "auto-failover" 功能。
	 * <p>
	 * 当某个节点宕掉时，Twemproxy可以自动将它从集群中剔除； 而当它恢复服务时，Twemproxy也会自动连接。
	 * <p>
	 * 随机样本 Keys 通过 {@link #sharding()} 方法自动生成，并写到 {@link #TWEMPROXY_FILE} 文件中。<br>
	 * <font color="red">但要注意：记得调整随机样本数量({@link #SIZE})的值！建议该值被设置为小于20</font>
	 */
	@Test
	public void autoFailover() {
		ShardedJedis jedis = null;
		String[] keys = {"-640595852", // 6379
				"563752978", // 6379
				"1297026184", // 6380
				"843626243", // 6380, 6379
				"579761811", // 6379
				"-1148481590", // 6379
				"-1653107798", // 6381
				"1208161125", // 6380, 6381
				"549094194", // 6381
				"1082579570", // 6380, 6381
		};
		String statusCode = null;

		for (String key : keys) {
			try {
				// 获取一条Redis连接
				jedis = this.pool.getResource();
				// SET
				statusCode = jedis.set(key, DEFAUL_VALUE);
				assertEquals(statusCode, RET_OK);
				// 返回连接到连接池
				jedis.close();
			} catch (Exception e) {
				logger.error("Set key error: {}", key);
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
