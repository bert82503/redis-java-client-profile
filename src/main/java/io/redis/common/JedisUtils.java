/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package io.redis.common;

import redis.clients.jedis.Jedis;

/**
 * Jedis utility class.
 * 
 * @author huagang.li 2014年12月13日 下午3:33:16
 */
public class JedisUtils {

    private static final String DEAD_SERVER_CLIENT_NAME = "DEAD";

    /**
     * 关闭一个给定的客户端连接。
     * 
     * @param jedis
     */
    public static void clientKill(Jedis jedis) {
        jedis.clientSetname(DEAD_SERVER_CLIENT_NAME);
        
        // CLIENT LIST (返回连接到这台服务器的所有客户端的信息和统计数据) - http://redis.io/commands/client-list
        // CLIENT LIST (多了name字段): "id=4 addr=127.0.0.1:50946 fd=6 name=DEAD age=0 idle=0 flags=N db=0 sub=0 psub=0 multi=-1 qbuf=0 qbuf-free=32768 obl=0 oll=0 omem=0 events=r cmd=client"
        for (String clientInfo : jedis.clientList().split("\n")) {
            if (clientInfo.contains(DEAD_SERVER_CLIENT_NAME)) {
                for (String field : clientInfo.split(" ")) {
                    if (field.contains("addr")) {
                        String hostAndPort = field.split("=")[1];
                        // It would be better if we kill the client by Id (CLIENT KILL ID client-id) as it's safer but Jedis doesn't implement the command yet.
                        // CLIENT KILL (关闭一个给定的客户端连接) - http://redis.io/commands/client-kill
                        jedis.clientKill(hostAndPort);
                    }
                }
            }
        }
    }

}
