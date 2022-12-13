package com.ltyzzz.core.proxy;

import com.ltyzzz.core.client.RpcReferenceWrapper;

public interface ProxyFactory {

    <T> T getProxy(RpcReferenceWrapper rpcReferenceWrapper) throws Throwable;
}
