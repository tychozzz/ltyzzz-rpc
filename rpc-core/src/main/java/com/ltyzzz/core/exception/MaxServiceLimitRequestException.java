package com.ltyzzz.core.exception;

import com.ltyzzz.core.common.RpcInvocation;

public class MaxServiceLimitRequestException extends IRpcException {

    public MaxServiceLimitRequestException(RpcInvocation rpcInvocation) {
        super(rpcInvocation);
    }
}
