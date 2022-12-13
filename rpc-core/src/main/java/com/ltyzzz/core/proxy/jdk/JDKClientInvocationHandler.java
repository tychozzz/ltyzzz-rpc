package com.ltyzzz.core.proxy.jdk;

import com.ltyzzz.core.client.RpcReferenceWrapper;
import com.ltyzzz.core.common.RpcInvocation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static com.ltyzzz.core.cache.CommonClientCache.RESP_MAP;
import static com.ltyzzz.core.cache.CommonClientCache.SEND_QUEUE;
import static com.ltyzzz.core.constants.RpcConstants.DEFAULT_TIMEOUT;

public class JDKClientInvocationHandler implements InvocationHandler {

    private final static Object OBJECT = new Object();

    private RpcReferenceWrapper rpcReferenceWrapper;

    private int timeOut = DEFAULT_TIMEOUT;

    public JDKClientInvocationHandler(RpcReferenceWrapper rpcReferenceWrapper) {
        this.rpcReferenceWrapper = rpcReferenceWrapper;
        timeOut = Integer.valueOf(String.valueOf(rpcReferenceWrapper.getAttachments().get("timeOut")));
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
        RESP_MAP.put(rpcInvocation.getUuid(), OBJECT);
        long beginTime = System.currentTimeMillis();
        int retryTimes = 0;
        // 超时判断
        timeOut = 1000000;
        while (System.currentTimeMillis() - beginTime < timeOut || rpcInvocation.getRetry() > 0) {
            Object object = RESP_MAP.get(rpcInvocation.getUuid());
            if (object instanceof RpcInvocation) {
                //return ((RpcInvocation) object).getResponse();
                RpcInvocation rpcInvocationResp = (RpcInvocation) object;
                if (rpcInvocationResp.getRetry() == 0 && rpcInvocationResp.getE() == null) {
                    return rpcInvocationResp.getResponse();
                } else if (rpcInvocationResp.getE() != null) {
                    if (rpcInvocationResp.getRetry() == 0) {
                        return rpcInvocationResp.getResponse();
                    }
                    if (System.currentTimeMillis() - beginTime > timeOut) {
                        retryTimes++;
                        rpcInvocation.setResponse(null);
                        rpcInvocation.setRetry(rpcInvocationResp.getRetry() - 1);
                        RESP_MAP.put(rpcInvocation.getUuid(), OBJECT);
                        SEND_QUEUE.add(rpcInvocation);
                    }
                }
            }
        }
        RESP_MAP.remove(rpcInvocation.getUuid());
        throw new TimeoutException("Wait for response from server on client " + timeOut + "ms,retry times is " + retryTimes + ",service's name is " + rpcInvocation.getTargetServiceName() + "#" + rpcInvocation.getTargetMethod());
    }
}
