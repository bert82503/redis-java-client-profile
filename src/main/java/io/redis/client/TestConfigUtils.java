/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package io.redis.client;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.redis.jedis.impl.RedisConfigUtils;

/**
 * 测试配置属性文件解析工具类。
 * 
 * @author huagang.li 2014年12月23日 下午3:48:20
 */
public class TestConfigUtils {

    private static final Logger logger       = LoggerFactory.getLogger(TestConfigUtils.class);

    private static final String REDIS_CONFIG = "properties/redis.properties";

    /**
     * 获取Redis服务器列表。
     * 
     * @return
     */
    public static String getRedisServers() {
        try {
            Properties configs = RedisConfigUtils.loadPropertyFile(REDIS_CONFIG);
            return configs.getProperty("redis.server.list");
        } catch (IOException e) {
            logger.error("Not found Redis config file: {}", REDIS_CONFIG);
        }
        return null;
    }

}
