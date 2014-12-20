/*
 * Copyright (c)
 */
package io.redis.client;

import static org.testng.Assert.assertEquals;
import io.redis.jedis.CustomShardedJedisPoolFactoryBean.PoolBehaviour;

import java.util.Arrays;

import org.testng.annotations.Test;

/**
 * Test for {@link PoolBehaviour}.
 * 
 * @author huagang.li
 */
public class PoolBehaviourTest {

	@Test
	public void values() {
		assertEquals(Arrays.toString(PoolBehaviour.values()), "[LIFO, FIFO]");
	}

}
