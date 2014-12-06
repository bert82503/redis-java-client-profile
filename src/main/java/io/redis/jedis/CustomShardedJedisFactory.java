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

    /** Jedis分片节点信息列表 */
    private List<JedisShardInfo>                  shards;
    /** 哈希算法 */
    private Hashing                               algo;
    /** 键标记模式 */
    private Pattern                               keyTagPattern;

    /** Redis节点到Jedis分片节点信息的映射表(<host:port, JedisShardInfo>) */
    private ConcurrentMap<String, JedisShardInfo> activeShardMap;

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

        activeShardMap = new ConcurrentHashMap<String, JedisShardInfo>();
        for (JedisShardInfo shardInfo : this.shards) {
            String shardKey = generateShardKey(shardInfo.getHost(), shardInfo.getPort());
            activeShardMap.put(shardKey, shardInfo);
        }
    }

    /*
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
     * 销毁整个{@link ShardedJedis}资源连接池。
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void destroyObject(PooledObject<ShardedJedis> pooledShardedJedis) throws Exception {
        final ShardedJedis shardedJedis = pooledShardedJedis.getObject();
        shardedJedis.disconnect();
    }

    /**
     * 校验整个{@link ShardedJedis}资源连接池中的所有是Jedis客户端链接否正常。
     * <p>
     * {@inheritDoc}
     */
    @Override
    public boolean validateObject(PooledObject<ShardedJedis> pooledShardedJedis) {
        Jedis jedis = null;
        try {
            ShardedJedis shardedJedis = pooledShardedJedis.getObject();
            List<Jedis> shards = new ArrayList<Jedis>(shardedJedis.getAllShards());
            int size = shards.size();
            for (int i = 0; i < size; i++) {
                jedis = shards.get(i);
                if (!jedis.ping().equals("PONG")) { // PING 命令
                    return false;
                }
            }
            return true;
        } catch (Exception ex) {
            // 移除出现异常的Redis节点
            this.removeShard(jedis);
            return false;
        }
    }

    /*
     * 移除一个异常的"Redis节点"信息。
     */
    private void removeShard(Jedis jedis) {
        Client redisClient = jedis.getClient();
        String shardKey = generateShardKey(redisClient.getHost(), redisClient.getPort());
        logger.warn("Remove a Redis server: {}", shardKey);
        JedisShardInfo shard = activeShardMap.remove(shardKey);
        if (null != shard) {
            shards = new ArrayList<JedisShardInfo>(activeShardMap.values());
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
