/*
 * Copyright (c)
 */
package io.redis.client;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;

/**
 * {@link RedisTemplate} tutorial.
 * <p>
 * {@link JedisConnectionFactory#afterPropertiesSet()} 会初始化一个基于 {@link JedisPool} 的 Redis连接池实现，
 * 这意味着不能实现 Redis集群操作。
 *
 * @author Bill
 * @version 
 * @since 
 */
public class RedisTemplateTest {

	private static final int DEFAULT_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(1L);
	
	private RedisTemplate<String, String> template;
	
	private JedisConnectionFactory connFactory;
	
	@BeforeClass
	public void init() {
		// 设置后端Redis服务器信息
		HostAndPort hostInfo = this.getHostInfo();
		JedisShardInfo shardInfo = new JedisShardInfo(hostInfo.getHost(), hostInfo.getPort(), DEFAULT_TIMEOUT);
		this.connFactory = new JedisConnectionFactory(shardInfo);
		// 设置Redis客户端连接池信息
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxTotal(10);
		poolConfig.setMinIdle(1);
		poolConfig.setTestOnBorrow(true);
		this.connFactory.setPoolConfig(poolConfig);
		// 设置连接超时时间
		this.connFactory.setTimeout(DEFAULT_TIMEOUT);
		
		// 初始化连接工厂(必须手动触发，因为未基于IoC容器生命周期实现)
		this.connFactory.afterPropertiesSet();
		
		// 初始化Redis模板
		this.template = new RedisTemplate<String, String>();
		this.template.setConnectionFactory(this.connFactory);
		this.template.afterPropertiesSet();
	}
	
	private HostAndPort getHostInfo() {
		String host = "127.0.0.1";
		int port = 10001;
		return new HostAndPort(host, port);
	}
	
	@Test
	public void doString() {
		 // TODO
	}
	
	@AfterClass
	public void destroy() {
		this.connFactory.destroy();
	}
	
}
