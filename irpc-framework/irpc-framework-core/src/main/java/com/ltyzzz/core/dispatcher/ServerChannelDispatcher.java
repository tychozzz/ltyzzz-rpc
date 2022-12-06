package com.ltyzzz.core.dispatcher;

import com.ltyzzz.core.common.RpcInvocation;
import com.ltyzzz.core.common.RpcProtocol;
import com.ltyzzz.core.server.ServerChannelReadData;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.*;

import static com.ltyzzz.core.cache.CommonServerCache.*;
import static com.ltyzzz.core.cache.CommonServerCache.SERVER_SERIALIZE_FACTORY;

public class ServerChannelDispatcher {

    private BlockingQueue<ServerChannelReadData> RPC_DATA_QUEUE;

    private ExecutorService executorService;

    public void init(int queueSize, int bizThreadNums) {
        RPC_DATA_QUEUE = new ArrayBlockingQueue<>(queueSize);
        executorService = new ThreadPoolExecutor(bizThreadNums, bizThreadNums, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(512));
    }

    public void add(ServerChannelReadData serverChannelReadData) {
        RPC_DATA_QUEUE.add(serverChannelReadData);
    }

    class ServerJobCoreHandle implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    ServerChannelReadData serverChannelReadData = RPC_DATA_QUEUE.take();
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                RpcProtocol rpcProtocol = serverChannelReadData.getRpcProtocol();
                                // 将msg的content内容进一步转换为RpcInvocation获得具体信息
                                RpcInvocation rpcInvocation = SERVER_SERIALIZE_FACTORY.deserialize(rpcProtocol.getContent(), RpcInvocation.class);
                                // 执行请求过滤
                                SERVER_FILTER_CHAIN.doFilter(rpcInvocation);
                                // 从服务提供者中获取目标服务
                                Object aimObject = PROVIDER_CLASS_MAP.get(rpcInvocation.getTargetServiceName());
                                // 获取目标服务的全部方法
                                Method[] methods = aimObject.getClass().getDeclaredMethods();
                                Object result = null;
                                // 寻找得到对应的方法并执行得到方法结果
                                for (Method method : methods) {
                                    if (method.getName().equals(rpcInvocation.getTargetMethod())) {
                                        if (method.getReturnType().equals(Void.TYPE)) {
                                            method.invoke(aimObject, rpcInvocation.getArgs());
                                        } else {
                                            result = method.invoke(aimObject, rpcInvocation.getArgs());
                                        }
                                        break;
                                    }
                                }
                                // 设置response
                                rpcInvocation.setResponse(result);
                                // 再次封装为RpcProtocol返回给客户端
                                RpcProtocol respRpcProtocol = new RpcProtocol(SERVER_SERIALIZE_FACTORY.serialize(rpcInvocation));
                                serverChannelReadData.getChannelHandlerContext().writeAndFlush(respRpcProtocol);
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            } catch (InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void startDataConsume() {
        Thread thread = new Thread(new ServerJobCoreHandle());
        thread.start();
    }
}
