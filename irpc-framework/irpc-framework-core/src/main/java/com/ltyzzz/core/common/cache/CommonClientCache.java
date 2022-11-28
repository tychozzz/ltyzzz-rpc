package com.ltyzzz.core.common.cache;


import com.ltyzzz.core.common.ChannelFutureWrapper;
import com.ltyzzz.core.common.RpcInvocation;
import com.ltyzzz.core.common.config.ClientConfig;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class CommonClientCache {

    public static BlockingQueue<RpcInvocation> SEND_QUEUE = new ArrayBlockingQueue(100);
    public static Map<String,Object> RESP_MAP = new ConcurrentHashMap<>();
    public static ClientConfig clientConfig;
    // 客户端订阅的服务
    public static List<String> SUBSCRIBE_SERVICE_LIST = new ArrayList<>();
    public static Set<String> SERVER_ADDRESS = new HashSet<>();
    // 远程调用map -> key:需要调用的serviceName value:与多个服务提供者建立的连接
    // 当客户端需要调用指定serviceName时，可以根据不同策略从多个服务提供者中选择一个
    public static Map<String, List<ChannelFutureWrapper>> CONNECT_MAP = new ConcurrentHashMap<>();
}
