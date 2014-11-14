//package io.redis.client;
//
//import static org.testng.Assert.assertEquals;
//
//import java.util.concurrent.TimeUnit;
//
//import org.redisson.Config;
//import org.redisson.MasterSlaveServersConfig;
//import org.redisson.Redisson;
//import org.redisson.SentinelServersConfig;
//import org.redisson.SingleServerConfig;
//import org.redisson.core.RBucket;
//import org.testng.annotations.Test;
//
///**
// * 类RedissonTest.java的实现描述：{@link Redisson} tutorial
// * 
// * @author huagang.li 2014年11月14日 上午11:03:05
// */
//public class RedissonTest {
//
//    private static final String DEFAULT_HOST  = "127.0.0.1:6379";
//
//    private static final String DEFAULT_KEY   = "hello";
//    private static final String DEFAULT_VALUE = "world";
//
//    private Redisson            redisson;
//
//    /**
//     * <font color="red">恐怖：耗时 3.066s！</font>
//     */
//    @Test
//    public void singleServer() {
//        Config config = new Config();
//        SingleServerConfig ssc = config.useSingleServer().setAddress(DEFAULT_HOST);
//        ssc.setConnectionPoolSize(3);
//        ssc.setRetryAttempts(3);
//        ssc.setRetryInterval((int) TimeUnit.MILLISECONDS.toMillis(250L));
//
//        redisson = Redisson.create(config);
//        // object
//        RBucket<String> bucket = redisson.getBucket(DEFAULT_KEY);
//        // bucket.set(DEFAULT_VALUE);
//        String value = bucket.get();
//        assertEquals(value, DEFAULT_VALUE);
//
//        // release resources
//        redisson.shutdown();
//    }
//
//    /**
//     * <font color="red">测试通过，但主库写不进去数据！</font>
//     * 
//     * @throws InterruptedException
//     */
//    @Test
//    public void masterSlaveServers() throws InterruptedException {
//        Config config = new Config();
//        MasterSlaveServersConfig mssc = config.useMasterSlaveConnection();
//        mssc.setMasterAddress(DEFAULT_HOST).addSlaveAddress("127.0.0.1:6380", "127.0.0.1:6381");
//        // mssc.setMasterConnectionPoolSize(3).setSlaveConnectionPoolSize(3);
//        // mssc.setRetryAttempts(3);
//        // mssc.setRetryInterval((int) TimeUnit.MILLISECONDS.toMillis(250L));
//
//        redisson = Redisson.create(config);
//        // object
//        RBucket<String> bucket = redisson.getBucket(DEFAULT_KEY);
//        // set - 写入主库
//        bucket.set(DEFAULT_VALUE);
//        // 主从同步，存在一定的延时
//        // TimeUnit.MILLISECONDS.sleep(500L);
//        // 从任意一个从库读取数据
//        // String value = bucket.get();
//        // assertEquals(value, DEFAULT_VALUE);
//
//        // release resources
//        redisson.shutdown();
//    }
//
//    @Test
//    public void sentinelServers() {
//        Config config = new Config();
//        SentinelServersConfig ssc = config.useSentinelConnection();
//        ssc.setMasterName("master").addSentinelAddress("127.0.0.1:6380", "127.0.0.1:6381");
//
//        redisson = Redisson.create(config);
//        // object
//        RBucket<String> bucket = redisson.getBucket(DEFAULT_KEY);
//        bucket.set(DEFAULT_VALUE);
//
//        // release resources
//        redisson.shutdown();
//    }
//
// }
