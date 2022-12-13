package com.ltyzzz.core.exception;

import com.ltyzzz.core.common.RpcInvocation;

public class IRpcException extends RuntimeException {

    private RpcInvocation rpcInvocation;

    public IRpcException(RpcInvocation rpcInvocation) {
        this.rpcInvocation = rpcInvocation;
    }

    public RpcInvocation getRpcInvocation() {
        return rpcInvocation;
    }

    public void setRpcInvocation(RpcInvocation rpcInvocation) {
        this.rpcInvocation = rpcInvocation;
    }
}
