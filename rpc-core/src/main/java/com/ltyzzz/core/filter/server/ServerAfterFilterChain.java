package com.ltyzzz.core.filter.server;

import com.ltyzzz.core.common.RpcInvocation;
import com.ltyzzz.core.filter.IServerFilter;

import java.util.ArrayList;
import java.util.List;

public class ServerAfterFilterChain {

    private static List<IServerFilter> iServerFilters = new ArrayList<>();

    public void addServerFilter(IServerFilter iServerFilter) {
        iServerFilters.add(iServerFilter);
    }

    public void doFilter(RpcInvocation rpcInvocation) {
        for (IServerFilter iServerFilter : iServerFilters) {
            iServerFilter.doFilter(rpcInvocation);
        }
    }
}
