package com.ltyzzz.core.proxy.javassist;


import com.ltyzzz.core.client.RpcReferenceWrapper;
import com.ltyzzz.core.proxy.ProxyFactory;

public class JavassistProxyFactory implements ProxyFactory {

    public JavassistProxyFactory() {
    }

    @Override
    public <T> T getProxy(RpcReferenceWrapper rpcReferenceWrapper) throws Throwable {
        return (T) ProxyGenerator.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                rpcReferenceWrapper.getAimClass(), new JavassistInvocationHandler(rpcReferenceWrapper));
    }
}
