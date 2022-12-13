package com.ltyzzz.core.filter.server;

import com.ltyzzz.core.annotations.SPI;
import com.ltyzzz.core.common.RpcInvocation;
import com.ltyzzz.core.common.ServerServiceSemaphoreWrapper;
import com.ltyzzz.core.filter.IServerFilter;

import static com.ltyzzz.core.cache.CommonServerCache.SERVER_SERVICE_SEMAPHORE_MAP;

@SPI("after")
public class ServerServiceAfterLimitFilterImpl implements IServerFilter {

    @Override
    public void doFilter(RpcInvocation rpcInvocation) {
        String serviceName = rpcInvocation.getTargetServiceName();
        ServerServiceSemaphoreWrapper serverServiceSemaphoreWrapper = SERVER_SERVICE_SEMAPHORE_MAP.get(serviceName);
        serverServiceSemaphoreWrapper.getSemaphore().release();
    }
}
