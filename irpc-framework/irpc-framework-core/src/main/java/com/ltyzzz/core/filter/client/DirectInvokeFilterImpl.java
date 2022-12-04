package com.ltyzzz.core.filter.client;

import com.ltyzzz.core.common.ChannelFutureWrapper;
import com.ltyzzz.core.common.RpcInvocation;
import com.ltyzzz.core.filter.IClientFilter;
import com.ltyzzz.core.utils.CommonUtils;

import java.util.Iterator;
import java.util.List;

public class DirectInvokeFilterImpl implements IClientFilter {

    @Override
    public void doFilter(List<ChannelFutureWrapper> src, RpcInvocation rpcInvocation) {
        String url = (String) rpcInvocation.getAttachments().get("url");
        if (CommonUtils.isEmpty(url)) return;
        Iterator<ChannelFutureWrapper> iterator = src.iterator();
        while (iterator.hasNext()) {
            ChannelFutureWrapper next = iterator.next();
            if (!(next.getHost() + ":" + next.getPort()).equals(url)) {
                iterator.remove();
            }
        }
        if(CommonUtils.isEmptyList(src)){
            throw new RuntimeException("no match provider url for "+ url);
        }
    }
}
