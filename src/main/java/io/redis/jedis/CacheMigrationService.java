/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package io.redis.jedis;

/**
 * 缓存迁移服务。
 * <p>
 * 缓存迁移方案：双写Memcached和Redis，读取通过开关从Memcached切换到Redis，最后固化Redis读取开关。
 * <p>
 * 【XML配置示例】
 * 
 * <pre>
 * {@literal
 * <bean id="cacheMigrationService" class="io.redis.jedis.impl.CacheMigrationServiceImpl">
 * }
 *    &lt;property name="readRedisEnabled" value="${cache.migration.read.redis.enabled}" />
 * {@literal
 * </bean>
 * }
 * </pre>
 * 
 * @author huagang.li 2014年12月19日 下午7:27:33
 */
public interface CacheMigrationService extends RedisServiceAdapter {

    /**
     * 设置"是否从Redis服务读取数据"开关。
     * 
     * @param enabled {@code true}：从Redis服务读取数据；{@code false}：从Memcached服务读取数据
     */
    void setReadRedisEnabled(boolean readRedisEnabled);

    /**
     * 获取"是否从Redis服务读取数据"开关的状态。
     * 
     * @return
     */
    boolean getReadRedisEnabled();

}
