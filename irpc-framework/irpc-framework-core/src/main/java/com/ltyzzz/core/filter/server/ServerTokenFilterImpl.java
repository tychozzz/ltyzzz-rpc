package com.ltyzzz.core.filter.server;

import com.ltyzzz.core.common.RpcInvocation;
import com.ltyzzz.core.filter.IServerFilter;
import com.ltyzzz.core.server.ServiceWrapper;
import com.ltyzzz.core.utils.CommonUtils;

import static com.ltyzzz.core.cache.CommonServerCache.PROVIDER_SERVICE_WRAPPER_MAP;

public class ServerTokenFilterImpl implements IServerFilter {

    // 接口级别的鉴权
    @Override
    public void doFilter(RpcInvocation rpcInvocation) {
        String token = String.valueOf(rpcInvocation.getAttachments().get("serviceToken"));
        ServiceWrapper serviceWrapper = PROVIDER_SERVICE_WRAPPER_MAP.get(rpcInvocation.getTargetServiceName());
        String matchToken = String.valueOf(serviceWrapper.getServiceToken());
        if (CommonUtils.isEmpty(matchToken)) return;
        if (!CommonUtils.isEmpty(token) && token.equals(matchToken)) return;
        throw new RuntimeException("token is " + token + " , verify result is false!");
    }
}
