/*
 * Copyright (c)
 */
package io.redis.client;

import static org.testng.Assert.assertEquals;

import java.util.Arrays;

import org.testng.annotations.Test;

import io.redis.jedis.impl.CustomShardedJedisPoolFactoryBean.PoolBehaviour;

/**
 * Test for {@link PoolBehaviour}.
 * 
 * @author huagang.li 2014年12月20日 上午11:18:59
 */
public class PoolBehaviourTest {

    @Test
    public void values() {
        assertEquals(Arrays.toString(PoolBehaviour.values()), "[LIFO, FIFO]");
    }

}
