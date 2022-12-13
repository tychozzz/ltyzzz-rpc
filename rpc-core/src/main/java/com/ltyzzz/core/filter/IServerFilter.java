package com.ltyzzz.core.filter;

import com.ltyzzz.core.common.RpcInvocation;

public interface IServerFilter extends IFilter {

    void doFilter(RpcInvocation rpcInvocation);
}
