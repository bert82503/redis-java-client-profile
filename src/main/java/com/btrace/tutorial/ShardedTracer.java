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
 * 追踪{@link redis.clients.util.Sharded Sharded}的实时状态。
 * 
 * <pre>
 * 【使用说明】
 *      1. 使用 jps 命令获取该Java应用程序的进程ID<pid>
 *      2. 使用如下命令运行本脚本程序。
 *           btrace 2803 /usr/apps/btrace/tutorial/ShardedTracer.java
 * </pre>
 * 
 * @author huagang.li 2014年12月6日 下午1:57:18
 */
@BTrace
public class ShardedTracer {

	@OnMethod(clazz = "redis.clients.util.Sharded", method = "initialize", location = @Location(Kind.RETURN))
	public static void onLog(@Self Object obj) {
		Field nodes = Reflective.field("redis.clients.util.Sharded", "nodes");
		println(get(nodes, obj));
	}

}
