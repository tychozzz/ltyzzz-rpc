package com.ltyzzz.core.filter.server;

import com.ltyzzz.core.annotations.SPI;
import com.ltyzzz.core.common.RpcInvocation;
import com.ltyzzz.core.common.ServerServiceSemaphoreWrapper;
import com.ltyzzz.core.exception.MaxServiceLimitRequestException;
import com.ltyzzz.core.filter.IServerFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;

import static com.ltyzzz.core.cache.CommonServerCache.SERVER_SERVICE_SEMAPHORE_MAP;

@SPI("before")
public class ServerServiceBeforeLimitFilterImpl implements IServerFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerServiceBeforeLimitFilterImpl.class);

    @Override
    public void doFilter(RpcInvocation rpcInvocation) {
        String serviceName = rpcInvocation.getTargetServiceName();
        ServerServiceSemaphoreWrapper serverServiceSemaphoreWrapper = SERVER_SERVICE_SEMAPHORE_MAP.get(serviceName);
        Semaphore semaphore = serverServiceSemaphoreWrapper.getSemaphore();
        boolean tryResult = semaphore.tryAcquire();
        if (!tryResult) {
            LOGGER.error("[ServerServiceBeforeLimitFilterImpl] {}'s max request is {},reject now", rpcInvocation.getTargetServiceName(), serverServiceSemaphoreWrapper.getMaxNums());
            MaxServiceLimitRequestException exception = new MaxServiceLimitRequestException(rpcInvocation);
            rpcInvocation.setE(exception);
            throw exception;
        }
    }
}
