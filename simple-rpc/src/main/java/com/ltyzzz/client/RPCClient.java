package com.ltyzzz.client;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;

public class RPCClient<T> {

    public static <T> T getRemoteProxyObj(final Object target, final InetSocketAddress addr) {
        JDKProxy jdkProxy = new JDKProxy(addr, target);
        return (T) Proxy.newProxyInstance(target.getClass().getClassLoader(),
                target.getClass().getInterfaces(), jdkProxy);
    }
}
