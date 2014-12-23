/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package io.redis.jedis;

import java.io.Closeable;

import io.redis.jedis.impl.CustomShardedJedisPool;

/**
 * Redis数据分片服务设置，并增加开关功能。
 * 
 * @author huagang.li 2014年12月19日 上午10:42:30
 */
public interface RedisShardedService extends Closeable {

    /**
     * 设置分片Redis连接池实例。
     * 
     * @param shardedJedisPool
     */
    void setShardedJedisPool(CustomShardedJedisPool shardedJedisPool);

    /**
     * 设置是否启用Redis服务。
     * 
     * @param enabled {@code true}：启用Redis服务；{@code false}：关闭Redis服务
     */
    void setEnabled(boolean enabled);

    /**
     * 关闭Redis连接池中所有客户端的链接。
     * <p>
     * {@inheritDoc}
     */
    @Override
    void close();

}
