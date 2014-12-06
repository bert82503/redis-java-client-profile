/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package io.redis.jedis;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.util.Hashing;
import redis.clients.util.Pool;

/**
 * "数据分片的Jedis连接池"自定义实现，继承自{@link Pool<ShardedJedis>}。
 * 
 * @author huagang.li 2014年12月4日 下午6:06:09
 */
public class CustomShardedJedisPool extends Pool<ShardedJedis> {

	/**
	 * 创建一个"数据分片的Jedis连接池"实例。
	 * 
	 * @param poolConfig
	 *            连接池配置信息
	 * @param shards
	 *            Jedis节点分片信息列表
	 */
	public CustomShardedJedisPool(GenericObjectPoolConfig poolConfig,
			List<JedisShardInfo> shards) {
		this(poolConfig, shards, Hashing.MURMUR_HASH);
	}

	public CustomShardedJedisPool(GenericObjectPoolConfig poolConfig,
			List<JedisShardInfo> shards, Hashing algo) {
		this(poolConfig, shards, algo, null);
	}

	public CustomShardedJedisPool(GenericObjectPoolConfig poolConfig,
			List<JedisShardInfo> shards, Pattern keyTagPattern) {
		this(poolConfig, shards, Hashing.MURMUR_HASH, keyTagPattern);
	}

	/**
	 * 创建一个"数据分片的Jedis连接池"实例，使用自定义实现的{@link CustomShardedJedisFactory}。
	 * 
	 * @param poolConfig
	 *            连接池配置信息
	 * @param shards
	 *            Jedis节点分片信息列表
	 * @param algo
	 *            哈希算法
	 * @param keyTagPattern
	 *            键标记模式
	 */
	public CustomShardedJedisPool(GenericObjectPoolConfig poolConfig,
			List<JedisShardInfo> shards, Hashing algo, Pattern keyTagPattern) {
		super(poolConfig, new CustomShardedJedisFactory(shards, algo,
				keyTagPattern));
	}

	/**
	 * 获取"Jedis连接池"中的一个{@link ShardedJedis}资源。
	 * 
	 * <pre>
	 * 分2个步骤：
	 * 	1. 从Pool<ShardedJedis>中获取一个{@link ShardedJedis}资源；
	 * 	2. 设置{@link ShardedJedis}资源所在的连接池数据源。
	 * </pre>
	 */
	@Override
	public ShardedJedis getResource() {
		ShardedJedis jedis = super.getResource();
		jedis.setDataSource(this);
		return jedis;
	}

	/**
	 * 将正常的{@link ShardedJedis}资源返回给"连接池"。
	 */
	@Override
	public void returnResource(ShardedJedis resource) {
		if (resource != null) {
			resource.resetState();
			this.returnResourceObject(resource);
		}
	}

	/**
	 * 将出现异常的{@link ShardedJedis}资源返回给"连接池"。
	 */
	@Override
	public void returnBrokenResource(ShardedJedis resource) {
		if (resource != null) {
			this.returnBrokenResourceObject(resource);
		}
	}

	/**
	 * 设置被遗弃的对象删除配置。
	 * 
	 * @param abandonedConfig
	 */
	public void setAbandonedConfig(AbandonedConfig abandonedConfig) {
		this.internalPool.setAbandonedConfig(abandonedConfig);
	}

}
