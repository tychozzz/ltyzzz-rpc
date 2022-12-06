package com.ltyzzz.core.filter.client;

import com.ltyzzz.core.common.ChannelFutureWrapper;
import com.ltyzzz.core.common.RpcInvocation;
import com.ltyzzz.core.filter.IClientFilter;
import com.ltyzzz.core.utils.CommonUtils;

import java.util.Iterator;
import java.util.List;

public class GroupFilterImpl implements IClientFilter {

    @Override
    public void doFilter(List<ChannelFutureWrapper> src, RpcInvocation rpcInvocation) {
        String group = String.valueOf(rpcInvocation.getAttachments().get("group"));
        Iterator<ChannelFutureWrapper> channelFutureWrapperIterator = src.iterator();
        while (channelFutureWrapperIterator.hasNext()){
            ChannelFutureWrapper channelFutureWrapper = channelFutureWrapperIterator.next();
            if (!channelFutureWrapper.getGroup().equals(group)) {
                channelFutureWrapperIterator.remove();
            }
        }
        if (CommonUtils.isEmptyList(src)) {
            throw new RuntimeException("no provider match for group " + group);
        }
    }
}
