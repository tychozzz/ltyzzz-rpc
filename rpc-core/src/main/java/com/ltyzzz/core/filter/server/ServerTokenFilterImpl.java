package com.ltyzzz.core.filter.server;

import com.ltyzzz.core.annotations.SPI;
import com.ltyzzz.core.common.RpcInvocation;
import com.ltyzzz.core.exception.IRpcException;
import com.ltyzzz.core.filter.IServerFilter;
import com.ltyzzz.core.server.ServiceWrapper;
import com.ltyzzz.core.utils.CommonUtils;

import static com.ltyzzz.core.cache.CommonClientCache.RESP_MAP;
import static com.ltyzzz.core.cache.CommonServerCache.PROVIDER_SERVICE_WRAPPER_MAP;

@SPI("before")
public class ServerTokenFilterImpl implements IServerFilter {

    // 接口级别的鉴权
    @Override
    public void doFilter(RpcInvocation rpcInvocation) {
        String token = String.valueOf(rpcInvocation.getAttachments().get("serviceToken"));
        ServiceWrapper serviceWrapper = PROVIDER_SERVICE_WRAPPER_MAP.get(rpcInvocation.getTargetServiceName());
        String matchToken = String.valueOf(serviceWrapper.getServiceToken());
        if (CommonUtils.isEmpty(matchToken)) return;
        if (!CommonUtils.isEmpty(token) && token.equals(matchToken)) return;
        rpcInvocation.setRetry(0);
        rpcInvocation.setE(new RuntimeException("service token is illegal for service " + rpcInvocation.getTargetServiceName()));
        rpcInvocation.setResponse(null);
        //直接交给响应线程那边处理（响应线程在代理类内部的invoke函数中，那边会取出对应的uuid的值，然后判断）
        RESP_MAP.put(rpcInvocation.getUuid(), rpcInvocation);
        throw new IRpcException(rpcInvocation);
    }
}
