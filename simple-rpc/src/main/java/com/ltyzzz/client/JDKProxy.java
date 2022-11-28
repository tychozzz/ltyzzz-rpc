package com.ltyzzz.client;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;

public class JDKProxy implements InvocationHandler {

    private InetSocketAddress addr;

    private Object target;

    public JDKProxy(InetSocketAddress addr, Object target) {
        this.addr = addr;
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Socket socket = new Socket();
        ObjectInputStream input = null;
        socket.connect(addr);
        try(ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {
            output.writeUTF(target.getClass().getName());
            output.writeUTF(method.getName());
            output.writeObject(method.getParameterTypes());
            output.writeObject(args);
            input = new ObjectInputStream(socket.getInputStream());
            return input.readObject();
        } finally {
            if (socket != null) socket.close();
            if (input != null) input.close();
        }
    }
}
