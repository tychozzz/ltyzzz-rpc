package com.ltyzzz.core.proxy;

import com.ltyzzz.core.client.RpcReferenceWrapper;
import com.ltyzzz.core.common.RpcInvocation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

import static com.ltyzzz.core.cache.CommonClientCache.RESP_MAP;
import static com.ltyzzz.core.cache.CommonClientCache.SEND_QUEUE;

public class JDKClientInvocationHandler implements InvocationHandler {

    private final static Object OBJECT = new Object();

    private RpcReferenceWrapper rpcReferenceWrapper;


    public JDKClientInvocationHandler(RpcReferenceWrapper rpcReferenceWrapper) {
        this.rpcReferenceWrapper = rpcReferenceWrapper;
    }

    /**
     * 封装RpcInvocation，并将其放入
     *   1.RESP_MAP -> 将请求与响应相关联，便于客户端接收结果时加以判断
     *   2.SEND_QUEUE -> 客户端中的异步线程会从阻塞队列中取出并按照顺序发送给服务端
     * @param proxy
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setTargetMethod(method.getName());
        rpcInvocation.setTargetServiceName(rpcReferenceWrapper.getAimClass().getName());
        rpcInvocation.setArgs(args);
        rpcInvocation.setUuid(UUID.randomUUID().toString());
        rpcInvocation.setAttachments(rpcReferenceWrapper.getAttachments());
        SEND_QUEUE.add(rpcInvocation);
        if (rpcReferenceWrapper.isAsync()) {
            return null;
        }
        long beginTime = System.currentTimeMillis();
        RESP_MAP.put(rpcInvocation.getUuid(), OBJECT);
        // 超时判断
        //while (System.currentTimeMillis() - beginTime < 3 * 1000) {
        while (true) {
            Object object = RESP_MAP.get(rpcInvocation.getUuid());
            if (object instanceof RpcInvocation) {
                return ((RpcInvocation) object).getResponse();
            }
        }
        //throw new TimeoutException("client wait server's response timeout!");
    }
}
