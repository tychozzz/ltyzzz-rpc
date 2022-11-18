package com.ltyzzz;

import com.ltyzzz.client.RPCClient;
import com.ltyzzz.server.Server;
import com.ltyzzz.server.ServiceCenter;
import com.ltyzzz.service.HelloService;
import com.ltyzzz.service.HelloServiceImpl;

import java.io.IOException;
import java.net.InetSocketAddress;

public class RPCTest {

    public static void main(String[] args) {
        new Thread(() -> {
            try {
                Server sererCenter = new ServiceCenter(8088);
                sererCenter.register(HelloServiceImpl.class, HelloServiceImpl.class);
                sererCenter.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        HelloService helloService = RPCClient.getRemoteProxyObj(new HelloServiceImpl(),
                        new InetSocketAddress("localhost", 8088));
        System.out.println(helloService.sayHi("ltyzzz"));
    }
}
