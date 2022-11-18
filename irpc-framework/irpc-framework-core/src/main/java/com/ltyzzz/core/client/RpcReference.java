package com.ltyzzz.core.client;

import com.ltyzzz.core.proxy.JDKProxyFactory;
import com.ltyzzz.core.proxy.ProxyFactory;

public class RpcReference {

    public ProxyFactory proxyFactory;

    public RpcReference(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    public <T> T getProxy(Class<T> tClass) throws Throwable {
        return proxyFactory.getProxy(tClass);
    }
}
