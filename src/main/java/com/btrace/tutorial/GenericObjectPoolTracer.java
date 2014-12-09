/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package com.btrace.tutorial;

import static com.sun.btrace.BTraceUtils.get;
import static com.sun.btrace.BTraceUtils.println;

import java.lang.reflect.Field;

import com.sun.btrace.BTraceUtils.Reflective;
import com.sun.btrace.annotations.BTrace;
import com.sun.btrace.annotations.Kind;
import com.sun.btrace.annotations.Location;
import com.sun.btrace.annotations.OnMethod;
import com.sun.btrace.annotations.Self;

/**
 * 追踪{@link org.apache.commons.pool2.impl.GenericObjectPool GenericObjectPool}
 * 的实时状态。
 * 
 * @author huagang.li 2014年12月8日 下午2:20:19
 */
@BTrace
public class GenericObjectPoolTracer {

	@OnMethod(clazz = "org.apache.commons.pool2.impl.GenericObjectPool", method = "addObject", location = @Location(Kind.RETURN))
	public static void addObject(@Self Object obj) {
		println("addObject(...):");

		Field allObjects = Reflective
				.field("org.apache.commons.pool2.impl.GenericObjectPool",
						"allObjects");
		println("All objects:");
		println(get(allObjects, obj));
		println();

		Field idleObjects = Reflective.field(
				"org.apache.commons.pool2.impl.GenericObjectPool",
				"idleObjects");
		println("Idle objects:");
		println(get(idleObjects, obj));
		println();
	}

	@OnMethod(clazz = "org.apache.commons.pool2.impl.GenericObjectPool", method = "borrowObject", location = @Location(Kind.RETURN))
	public static void borrowObject(@Self Object obj) {
		println("borrowObject(...):");

		Field allObjects = Reflective
				.field("org.apache.commons.pool2.impl.GenericObjectPool",
						"allObjects");
		println("All objects:");
		println(get(allObjects, obj));
		println();

		Field idleObjects = Reflective.field(
				"org.apache.commons.pool2.impl.GenericObjectPool",
				"idleObjects");
		println("Idle objects:");
		println(get(idleObjects, obj));
		println();
	}

}
