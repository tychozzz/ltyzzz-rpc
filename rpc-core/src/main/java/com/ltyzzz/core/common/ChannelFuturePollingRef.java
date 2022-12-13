package com.ltyzzz.core.common;

import com.ltyzzz.core.router.Selector;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ChannelFuturePollingRef {

    private Map<String, AtomicLong> referenceMap = new ConcurrentHashMap<>();

    public ChannelFutureWrapper getChannelFutureWrapper(Selector selector) {
        //ChannelFutureWrapper[] arr = SERVICE_ROUTER_MAP.get(serviceName);
        AtomicLong referCount = referenceMap.get(selector.getProviderServiceName());
        if (referCount == null) {
            referCount = new AtomicLong(0);
            referenceMap.put(selector.getProviderServiceName(), referCount);
        }
        ChannelFutureWrapper[] arr = selector.getChannelFutureWrappers();
        long i = referCount.getAndIncrement();
        int index = (int) (i % arr.length);
        return arr[index];
    }
}
