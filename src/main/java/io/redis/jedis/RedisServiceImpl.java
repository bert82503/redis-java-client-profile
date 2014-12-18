/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package io.redis.jedis;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.exceptions.JedisException;
import io.redis.jedis.RedisService;

/**
 * "数据分片的Jedis连接池"服务实现类，继承自{@link RedisService}。
 * 
 * @author huagang.li 2014年12月12日 下午4:59:38
 */
public class RedisServiceImpl implements RedisService, Closeable {

    private static final Logger    logger = LoggerFactory.getLogger(RedisServiceImpl.class);

    /** Redis连接池 */
    @Autowired
    private CustomShardedJedisPool shardedJedisPool;

    /** Redis集群开关是否可用标识 */
    private boolean                enabled;

    public void setShardedJedisPool(CustomShardedJedisPool shardedJedisPool) {
        this.shardedJedisPool = shardedJedisPool;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void close() {
        shardedJedisPool.close();
    }

    /**
     * FIXME 不清楚这里为什么要过滤？
     * 
     * @param key
     * @return
     */
    private static String trim(String key) {
        return key.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5\\.\\-\\_]", "");
    }

    @Override
    public String get(String key) {
        if (enabled && StringUtils.isNotBlank(key)) {
            key = trim(key);
            try {
                ShardedJedis jedis = shardedJedisPool.getResource();
                String value = jedis.get(key);
                jedis.close();
                return value;
            } catch (JedisException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }

    @Override
    public String set(String key, String value) {
        if (enabled && StringUtils.isNotBlank(key)) {
            key = trim(key);
            try {
                ShardedJedis jedis = shardedJedisPool.getResource();
                String ret = jedis.set(key, value);
                jedis.close();
                return ret;
            } catch (JedisException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }

    @Override
    public String setex(String key, int seconds, String value) {
        if (enabled && StringUtils.isNotBlank(key)) {
            key = trim(key);
            try {
                ShardedJedis jedis = shardedJedisPool.getResource();
                String ret = jedis.setex(key, seconds, value);
                jedis.close();
                return ret;
            } catch (JedisException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }

    @Override
    public long llen(String key) {
        if (enabled && StringUtils.isNotBlank(key)) {
            key = trim(key);
            try {
                ShardedJedis jedis = shardedJedisPool.getResource();
                long listLength = jedis.llen(key).longValue();
                jedis.close();
                return listLength;
            } catch (JedisException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return 0L;
    }

    @Override
    public long lpush(String key, String... values) {
        if (enabled && StringUtils.isNotBlank(key)) {
            key = trim(key);
            try {
                ShardedJedis jedis = shardedJedisPool.getResource();
                long pushedListLength = jedis.lpush(key, values).longValue();
                jedis.close();
                return pushedListLength;
            } catch (JedisException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return 0L;
    }

    @Override
    public String rpop(String key) {
        if (enabled && StringUtils.isNotBlank(key)) {
            key = trim(key);
            try {
                ShardedJedis jedis = shardedJedisPool.getResource();
                String value = jedis.rpop(key);
                jedis.close();
                return value;
            } catch (JedisException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }

    @Override
    public List<String> lrange(String key, long start, long stop) {
        if (enabled && StringUtils.isNotBlank(key)) {
            key = trim(key);
            try {
                ShardedJedis jedis = shardedJedisPool.getResource();
                List<String> list = jedis.lrange(key, start, stop);
                jedis.close();
                return list;
            } catch (JedisException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return Collections.emptyList();
    }

    @Override
    public String ltrim(String key, long start, long stop) {
        if (enabled && StringUtils.isNotBlank(key)) {
            key = trim(key);
            try {
                ShardedJedis jedis = shardedJedisPool.getResource();
                String ret = jedis.ltrim(key, start, stop);
                jedis.close();
                return ret;
            } catch (JedisException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }

    @Override
    public long zadd(String key, double score, String member) {
        if (enabled && StringUtils.isNotBlank(key)) {
            key = trim(key);
            try {
                ShardedJedis jedis = shardedJedisPool.getResource();
                long newElementNum = jedis.zadd(key, score, member).longValue();
                jedis.close();
                return newElementNum;
            } catch (JedisException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return 0L;
    }

    @Override
    public long zadd(String key, Map<String, Double> scoreMembers) {
        if (enabled && StringUtils.isNotBlank(key)) {
            key = trim(key);
            try {
                ShardedJedis jedis = shardedJedisPool.getResource();
                long newElementNum = jedis.zadd(key, scoreMembers).longValue();
                jedis.close();
                return newElementNum;
            } catch (JedisException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return 0L;
    }

    @Override
    public Set<String> zrangeByScore(String key, double min, double max) {
        if (enabled && StringUtils.isNotBlank(key)) {
            key = trim(key);
            try {
                ShardedJedis jedis = shardedJedisPool.getResource();
                Set<String> zset = jedis.zrangeByScore(key, min, max);
                jedis.close();
                return zset;
            } catch (JedisException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return Collections.emptySet();
    }

    @Override
    public Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
        if (enabled && StringUtils.isNotBlank(key)) {
            key = trim(key);
            try {
                ShardedJedis jedis = shardedJedisPool.getResource();
                Set<String> zset = jedis.zrangeByScore(key, min, max, offset, count);
                jedis.close();
                return zset;
            } catch (JedisException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return Collections.emptySet();
    }

    @Override
    public Set<String> zrevrangeByScore(String key, double max, double min) {
        if (enabled && StringUtils.isNotBlank(key)) {
            key = trim(key);
            try {
                ShardedJedis jedis = shardedJedisPool.getResource();
                Set<String> zset = jedis.zrevrangeByScore(key, max, min);
                jedis.close();
                return zset;
            } catch (JedisException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return Collections.emptySet();
    }

    @Override
    public Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
        if (enabled && StringUtils.isNotBlank(key)) {
            key = trim(key);
            try {
                ShardedJedis jedis = shardedJedisPool.getResource();
                Set<String> zset = jedis.zrevrangeByScore(key, max, min, offset, count);
                jedis.close();
                return zset;
            } catch (JedisException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return Collections.emptySet();
    }

    @Override
    public long zcard(String key) {
        if (enabled && StringUtils.isNotBlank(key)) {
            key = trim(key);
            try {
                ShardedJedis jedis = shardedJedisPool.getResource();
                long zsetElementNum = jedis.zcard(key).longValue();
                jedis.close();
                return zsetElementNum;
            } catch (JedisException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return 0L;
    }

    @Override
    public long zremrangeByScore(String key, double min, double max) {
        if (enabled && StringUtils.isNotBlank(key)) {
            key = trim(key);
            try {
                ShardedJedis jedis = shardedJedisPool.getResource();
                long removedElementNum = jedis.zremrangeByScore(key, min, max);
                jedis.close();
                return removedElementNum;
            } catch (JedisException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return 0L;
    }

}
