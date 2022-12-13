package com.ltyzzz.core.client;

import com.alibaba.fastjson.JSON;
import com.ltyzzz.core.common.RpcDecoder;
import com.ltyzzz.core.common.RpcEncoder;
import com.ltyzzz.core.common.RpcInvocation;
import com.ltyzzz.core.common.RpcProtocol;
import com.ltyzzz.core.config.ClientConfig;
import com.ltyzzz.core.config.PropertiesBootstrap;
import com.ltyzzz.core.filter.IClientFilter;
import com.ltyzzz.core.filter.client.ClientFilterChain;
import com.ltyzzz.core.proxy.ProxyFactory;
import com.ltyzzz.core.registry.RegistryService;
import com.ltyzzz.core.router.IRouter;
import com.ltyzzz.core.serialize.SerializeFactory;
import com.ltyzzz.core.utils.CommonUtils;
import com.ltyzzz.core.event.IRpcListenerLoader;
import com.ltyzzz.core.registry.URL;
import com.ltyzzz.core.registry.zookeeper.AbstractRegister;
import com.ltyzzz.core.service.DataService;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.ltyzzz.core.cache.CommonClientCache.*;
import static com.ltyzzz.core.constants.RpcConstants.*;
import static com.ltyzzz.core.spi.ExtensionLoader.EXTENSION_LOADER_CLASS_CACHE;

public class Client {

    private Logger logger = LoggerFactory.getLogger(Client.class);

    private ClientConfig clientConfig;

    private IRpcListenerLoader iRpcListenerLoader;

    private Bootstrap bootstrap = new Bootstrap();

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public void setClientConfig(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    public RpcReference initClientApplication() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        EventLoopGroup clientGroup = new NioEventLoopGroup();
        bootstrap.group(clientGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ByteBuf delimiter = Unpooled.copiedBuffer(DEFAULT_DECODE_CHAR.getBytes());
                ch.pipeline().addLast(new DelimiterBasedFrameDecoder(clientConfig.getMaxServerRespDataSize(), delimiter));
                ch.pipeline().addLast(new RpcEncoder());
                ch.pipeline().addLast(new RpcDecoder());
                ch.pipeline().addLast(new ClientHandler());
            }
        });
        iRpcListenerLoader = new IRpcListenerLoader();
        iRpcListenerLoader.init();
        this.clientConfig = PropertiesBootstrap.loadClientConfigFromLocal();
        CLIENT_CONFIG = this.clientConfig;
        this.initClientConfig();
        EXTENSION_LOADER.loadExtension(ProxyFactory.class);
        String proxyType = clientConfig.getProxyType();
        LinkedHashMap<String, Class> classMap = EXTENSION_LOADER_CLASS_CACHE.get(ProxyFactory.class.getName());
        Class proxyTypeClass = classMap.get(proxyType);
        ProxyFactory proxyFactory = (ProxyFactory) proxyTypeClass.newInstance();
        return new RpcReference(proxyFactory);
    }

    public void doSubscribeService(Class serviceBean) {
        if (ABSTRACT_REGISTER == null) {
            try {
                EXTENSION_LOADER.loadExtension(RegistryService.class);
                LinkedHashMap<String, Class> registerMap = EXTENSION_LOADER_CLASS_CACHE.get(RegistryService.class.getName());
                Class registerClass = registerMap.get(clientConfig.getRegisterType());
                ABSTRACT_REGISTER = (AbstractRegister) registerClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("registryServiceType unKnow,error is ", e);
            }
        }
        URL url = new URL();
        url.setApplicationName(clientConfig.getApplicationName());
        url.setServiceName(serviceBean.getName());
        url.addParameter("host", CommonUtils.getIpAddress());
        Map<String, String> result = ABSTRACT_REGISTER.getServiceWeightMap(serviceBean.getName());
        URL_MAP.put(serviceBean.getName(), result);
        ABSTRACT_REGISTER.subscribe(url);
    }

    public void doConnectServer() {
        for (URL providerURL : SUBSCRIBE_SERVICE_LIST) {
            List<String> providerIps = ABSTRACT_REGISTER.getProviderIps(providerURL.getServiceName());
            for (String providerIp : providerIps) {
                try {
                    ConnectionHandler.connect(providerURL.getServiceName(), providerIp);
                } catch (InterruptedException e) {
                    logger.error("[doConnectServer] connect fail ", e);
                }
            }
            URL url = new URL();
            url.addParameter("servicePath", providerURL.getServiceName() + "/provider");
            url.addParameter("providerIps", JSON.toJSONString(providerIps));
            // 开启监听
            ABSTRACT_REGISTER.doAfterSubscribe(url);
        }
    }

    public void startClient() {
        Thread asyncSendJob = new Thread(new AsyncSendJob());
        asyncSendJob.start();
    }

    class AsyncSendJob implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    RpcInvocation rpcInvocation = SEND_QUEUE.take();
                    ChannelFuture channelFuture = ConnectionHandler.getChannelFuture(rpcInvocation);
                    if (channelFuture != null) {
                        Channel channel = channelFuture.channel();
                        //如果出现服务端中断的情况需要兼容下
                        if (!channel.isOpen()) {
                            throw new RuntimeException("aim channel is not open!rpcInvocation is " + rpcInvocation);
                        }
                        RpcProtocol rpcProtocol = new RpcProtocol(CLIENT_SERIALIZE_FACTORY.serialize(rpcInvocation));
                        channel.writeAndFlush(rpcProtocol);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //public static void main(String[] args) throws Throwable {
    //    Client client = new Client();
    //    RpcReference rpcReference = client.initClientApplication();
    //    RpcReferenceWrapper<DataService> rpcReferenceWrapper = new RpcReferenceWrapper<>();
    //    rpcReferenceWrapper.setAimClass(DataService.class);
    //    rpcReferenceWrapper.setGroup("dev");
    //    rpcReferenceWrapper.setServiceToken("token-a");
    //    DataService dataService = rpcReference.get(rpcReferenceWrapper);
    //    client.doSubscribeService(DataService.class);
    //    ConnectionHandler.setBootstrap(client.getBootstrap());
    //    client.doConnectServer();
    //    client.startClient();
    //    for (int i = 0; i < 100; i++) {
    //        try {
    //            // 调用方法时才开始进行网络通信
    //            String result = dataService.sendData("test");
    //            System.out.println(result);
    //            Thread.sleep(1000);
    //        } catch (Exception e) {
    //            e.printStackTrace();
    //        }
    //    }
    //}

    private void initClientConfig() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        //初始化路由策略 多选一
        EXTENSION_LOADER.loadExtension(IRouter.class);
        String routerStrategy = clientConfig.getRouterStrategy();
        LinkedHashMap<String, Class> iRouterMap = EXTENSION_LOADER_CLASS_CACHE.get(IRouter.class.getName());
        Class iRouterClass = iRouterMap.get(routerStrategy);
        if (iRouterClass == null) {
            throw new RuntimeException("no match routerStrategy for " + routerStrategy);
        }
        IROUTER = (IRouter) iRouterClass.newInstance();
        // 初始化序列化框架 多选一
        EXTENSION_LOADER.loadExtension(SerializeFactory.class);
        String clientSerialize = clientConfig.getClientSerialize();
        LinkedHashMap<String, Class> serializeMap = EXTENSION_LOADER_CLASS_CACHE.get(SerializeFactory.class.getName());
        Class serializeFactoryClass = serializeMap.get(clientSerialize);
        if (serializeFactoryClass == null) {
            throw new RuntimeException("no match serialize type for " + clientSerialize);
        }
        CLIENT_SERIALIZE_FACTORY = (SerializeFactory) serializeFactoryClass.newInstance();
        // 初始化过滤链 全部添加
        EXTENSION_LOADER.loadExtension(IClientFilter.class);
        ClientFilterChain clientFilterChain = new ClientFilterChain();
        LinkedHashMap<String, Class> iClientMap = EXTENSION_LOADER_CLASS_CACHE.get(IClientFilter.class.getName());
        for (String implClassName : iClientMap.keySet()) {
            Class iClientFilterClass = iClientMap.get(implClassName);
            if (iClientFilterClass == null) {
                throw new RuntimeException("no match iClientFilter for " + iClientFilterClass);
            }
            clientFilterChain.addClientFilter((IClientFilter) iClientFilterClass.newInstance());
        }
        CLIENT_FILTER_CHAIN = clientFilterChain;
    }
}
