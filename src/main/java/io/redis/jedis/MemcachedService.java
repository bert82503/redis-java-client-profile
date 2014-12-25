/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package io.redis.jedis;

import java.util.concurrent.Future;

/**
 * 对Memcached客户端的封装，增加开关的设置。
 * @author huagang.li 2014年12月23日 下午6:05:31
 */
public interface MemcachedService {

    Object get(String key);

    String getString(String key);

    Future<Object> getAsync(String key);

    Future<Boolean> set(String key, int exp, Object value);

    Future<Boolean> set(String key, int exp, String value);

    Future<Boolean> append(String key, int exp, String value);

    Future<Boolean> delete(String key);

}
