/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package io.redis.jedis.impl;

import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import io.redis.jedis.RedisService;
import io.redis.jedis.RedisServiceAdapter;

/**
 * {@link RedisServiceAdapter}实现类。
 * 
 * @author huagang.li 2014年12月17日 上午11:36:35
 */
public class RedisServiceAdapterImpl implements RedisServiceAdapter {

    private static final RedisFuture<Boolean> FUTURE_TRUE = new RedisFuture<Boolean>(Boolean.TRUE);

    @Autowired
    private RedisService                      redisService;

    @Override
    public Object get(String key) {
        return redisService.get(key);
    }

    @Override
    public String getString(String key) {
        return redisService.get(key);
    }

    /**
     * 不支持该操作，因为通过Eclipse的调用层次结构(Call Hierarchy)功能已核实
     * {@link cn.fraudmetrix.forseti.biz.service.intf.MemcachedService#set(String, int, Object)}方法并未在项目中使用。
     */
    @Override
    public Future<Boolean> set(String key, int exp, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<Boolean> set(String key, int exp, String value) {
        if (exp <= 0) {
            redisService.set(key, value);
        } else {
            redisService.setex(key, exp, value);
        }
        return FUTURE_TRUE;
    }

    @Override
    public Future<Boolean> append(String key, int exp, String value) {
        if (StringUtils.isNotBlank(key) && null != value) {
            String origin = this.getString(key);
            String newValue = (origin == null) ? value : (origin + value);
            if (exp <= 0) {
                redisService.set(key, newValue);
            } else {
                redisService.setex(key, exp, newValue);
            }
        }
        return FUTURE_TRUE;
    }

    @Override
    public Future<Boolean> delete(String key) {
        if (StringUtils.isNotBlank(key)) {
            long removedKeyNum = redisService.del(key);
            if (removedKeyNum > 0) {
                return FUTURE_TRUE;
            }
        }
        return null;
    }

    @Override
    public Future<Object> getAsync(String key) {
        if (StringUtils.isNotBlank(key)) {
            String value = redisService.get(key);
            if (null != value) {
                return new RedisFuture<Object>(value);
            }
        }
        return null;
    }

}
