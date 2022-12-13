package com.ltyzzz.core.filter;

import com.ltyzzz.core.common.ChannelFutureWrapper;
import com.ltyzzz.core.common.RpcInvocation;

import java.util.List;

public interface IClientFilter extends IFilter {

    void doFilter(List<ChannelFutureWrapper> src, RpcInvocation rpcInvocation);
}
