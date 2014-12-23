/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package io.redis.jedis;

import io.redis.jedis.MemcachedService;

/**
 * Redis服务适配器，适配{@link MemcachedService}接口，用于迁移Memcached到Redis。
 * <p>
 * 【XML配置示例】
 * 
 * <pre>
 * {@literal 
 * <bean id="redisServiceAdapter" class="io.redis.jedis.impl.RedisServiceAdapterImpl" />
 * }
 * </pre>
 * 
 * @author huagang.li 2014年12月17日 上午11:17:53
 */
public interface RedisServiceAdapter extends MemcachedService {

}
