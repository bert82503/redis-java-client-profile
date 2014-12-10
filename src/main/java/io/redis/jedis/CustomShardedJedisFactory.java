/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package io.redis.jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.util.Hashing;

/**
 * {@link PooledObjectFactory<ShardedJedis>}自定义实现类。
 * 
 * @author huagang.li 2014年12月4日 下午7:58:03
 */
public class CustomShardedJedisFactory implements PooledObjectFactory<ShardedJedis> {

    private static final Logger                   logger = LoggerFactory.getLogger(CustomShardedJedisFactory.class);

    /** 正常活跃的Jedis分片节点信息列表 */
    private List<JedisShardInfo>                  shards;
    /** 哈希算法 */
    private Hashing                               algo;
    /** 键标记模式 */
    private Pattern                               keyTagPattern;
    /** 初始节点个数 */
    private final int                             initialShardNumber;

    /** 正常活跃的Jedis分片节点信息映射表(<host:port, JedisShardInfo>) */
    private ConcurrentMap<String, JedisShardInfo> activeShardMap;
    /** 异常的Jedis分片节点列表 */
    private ConcurrentMap<Jedis, JedisShardInfo>  brokenShardMap;

    /**
     * 创建一个"数据分片的Jedis工厂"实例。
     * 
     * @param shards Jedis分片节点信息列表
     * @param algo 哈希算法
     * @param keyTagPattern 键标记模式
     */
    public CustomShardedJedisFactory(List<JedisShardInfo> shards, Hashing algo, Pattern keyTagPattern){
        this.shards = shards;
        this.algo = algo;
        this.keyTagPattern = keyTagPattern;
        this.initialShardNumber = this.shards.size();

        if (logger.isDebugEnabled()) {
            logger.debug("Initial Shard List: {}", this.shards);
        }

        int initialCapacity = shards.size() * 4 / 3 + 1;
        activeShardMap = new ConcurrentHashMap<String, JedisShardInfo>(initialCapacity);
        for (JedisShardInfo shardInfo : this.shards) {
            String shardKey = generateShardKey(shardInfo.getHost(), shardInfo.getPort());
            activeShardMap.put(shardKey, shardInfo);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Initial active Shard map: {}", activeShardMap);
        }

        brokenShardMap = new ConcurrentHashMap<Jedis, JedisShardInfo>(3);
    }

    /**
     * 生成Shard键，key格式是 "host:port"。
     */
    private static String generateShardKey(String host, int port) {
        return host + ':' + port;
    }

    /**
     * 创建一个{@link ShardedJedis}资源实例，并将它包装在{@link PooledObject}里便于连接池管理。
     * <p>
     * {@inheritDoc}
     */
    @Override
    public PooledObject<ShardedJedis> makeObject() throws Exception {
        ShardedJedis jedis = new ShardedJedis(shards, algo, keyTagPattern);
        return new DefaultPooledObject<ShardedJedis>(jedis);
    }

    /**
     * 销毁这个{@link PooledObject<ShardedJedis>}池对象。
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void destroyObject(PooledObject<ShardedJedis> pooledShardedJedis) throws Exception {
        final ShardedJedis shardedJedis = pooledShardedJedis.getObject();
        shardedJedis.disconnect();
    }

    /**
     * 校验整个{@link ShardedJedis}集群中所有的Jedis链接是否正常。
     * <p>
     * <font color="red">这个操作是挺耗时的！</font>
     * <p>
     * {@inheritDoc}
     */
    @Override
    public boolean validateObject(PooledObject<ShardedJedis> pooledShardedJedis) {
        Jedis jedis = null;
        try {
            ShardedJedis shardedJedis = pooledShardedJedis.getObject();

            // 每次校验"Jedis集群池对象"有效性时，都会对所有Redis服务器进行"PING命令"请求，这样是很耗时的！
            List<Jedis> shards = new ArrayList<Jedis>(shardedJedis.getAllShards());
            if (logger.isDebugEnabled()) {
                logger.debug("Active Shard List for current validated sharded Jedis: {}", listShardsToString(shards));
            }

            int size = shards.size();
            for (int i = 0; i < size; i++) {
                jedis = shards.get(i);
                if (!jedis.ping().equals("PONG")) { // PING 命令
                    return false;
                }
            }

            // broken server 自动探测
            if (initialShardNumber > shards.size() && !brokenShardMap.isEmpty()) {
                boolean hasNormalShard = false;
                for (Jedis shard : brokenShardMap.keySet()) {
                    try {
                        if (shard.ping().equals("PONG")) { // PING 命令
                            // 探测到一个异常节点现在恢复了
                            // JedisShardInfo normalShard = brokenShardMap.remove(shard);
                            JedisShardInfo normalShard = brokenShardMap.get(shard);
                            logger.warn("Broken Shard server now is normal: {}", normalShard);
                            if (initialShardNumber > this.shards.size()) {
                                this.shards.add(normalShard);
                                String shardKey = generateShardKey(normalShard.getHost(), normalShard.getPort());
                                this.activeShardMap.put(shardKey, normalShard);

                                if (logger.isDebugEnabled()) {
                                    logger.info("Active Shard list after a return to normal node added: {}",
                                                this.shards);
                                    logger.info("Active Shard map after a return to normal node added: {}",
                                                this.activeShardMap);
                                }
                            }
                            hasNormalShard = true;
                        }
                    } catch (Exception e) {
                        // 探测异常的节点，抛异常是正常行为，忽略之。
                        // logger.warn("Failed to detect to Broken Shard", e);
                    }
                }
                if (hasNormalShard) {
                    return false;
                }
            }

            return true;
        } catch (Exception ex) {
            // 摘除出现异常的Redis节点
            this.removeShard(jedis);
            return false;
        }
    }

    private static String listShardsToString(List<Jedis> shards) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (Jedis jedis : shards) {
            Client client = jedis.getClient();
            sb.append(client.getHost()).append(':').append(client.getPort()).append(',');
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * 从集群中摘除异常的"Redis节点"。
     */
    private void removeShard(Jedis jedis) {
        Client redisClient = jedis.getClient();
        redisClient.close();
        String shardKey = generateShardKey(redisClient.getHost(), redisClient.getPort());
        JedisShardInfo shard = activeShardMap.remove(shardKey);
        if (null != shard) { // 节点已不在活跃节点列表中，这样保证只会被移出一次
            logger.warn("Remove a broken Redis server: {}", shardKey);

            shards = new ArrayList<JedisShardInfo>(activeShardMap.values());
            brokenShardMap.put(jedis, shard);

            if (logger.isDebugEnabled()) {
                logger.debug("Active Shard List after a broken Redis server removed: {}", shards);
            }
        }
    }

    /**
     * 重新初始化{@link ShardedJedis}连接池对象，并返回给连接池。
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void activateObject(PooledObject<ShardedJedis> p) throws Exception {
        //
    }

    /**
     * 不初始化{@link ShardedJedis}连接池对象，并返回到空闲对象池(idleObjects)。
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void passivateObject(PooledObject<ShardedJedis> p) throws Exception {
        //
    }

}
