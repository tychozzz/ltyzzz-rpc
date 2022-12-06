package com.ltyzzz.core.server;

import com.ltyzzz.core.common.RpcDecoder;
import com.ltyzzz.core.common.RpcEncoder;
import com.ltyzzz.core.config.PropertiesBootstrap;
import com.ltyzzz.core.config.ServerConfig;
import com.ltyzzz.core.filter.IServerFilter;
import com.ltyzzz.core.filter.server.ServerFilterChain;
import com.ltyzzz.core.registry.RegistryService;
import com.ltyzzz.core.serialize.SerializeFactory;
import com.ltyzzz.core.serialize.fastjson.FastJsonSerializeFactory;
import com.ltyzzz.core.serialize.hessian.HessianSerializeFactory;
import com.ltyzzz.core.serialize.jdk.JdkSerializeFactory;
import com.ltyzzz.core.serialize.kryo.KryoSerializeFactory;
import com.ltyzzz.core.utils.CommonUtils;
import com.ltyzzz.core.event.IRpcListenerLoader;
import com.ltyzzz.core.registry.URL;
import com.ltyzzz.core.registry.zookeeper.ZookeeperRegister;
import com.ltyzzz.core.service.DataServiceImpl;
import com.ltyzzz.core.service.UserServiceImpl;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.IOException;
import java.util.LinkedHashMap;

import static com.ltyzzz.core.cache.CommonClientCache.EXTENSION_LOADER;
import static com.ltyzzz.core.cache.CommonServerCache.*;
import static com.ltyzzz.core.constants.RpcConstants.*;
import static com.ltyzzz.core.spi.ExtensionLoader.EXTENSION_LOADER_CLASS_CACHE;

public class Server {

    private static EventLoopGroup bossGroup = null;
    private static EventLoopGroup workerGroup = null;
    private ServerConfig serverConfig;
    private static IRpcListenerLoader iRpcListenerLoader;

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public void startApplication() throws InterruptedException {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_SNDBUF, 16 * 1024)
                .option(ChannelOption.SO_RCVBUF, 16 * 1024)
                .option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel channel) throws Exception {
                System.out.println("初始化provider");
                channel.pipeline().addLast(new RpcEncoder());
                channel.pipeline().addLast(new RpcDecoder());
                channel.pipeline().addLast(new ServerHandler());
            }
        });
        this.batchExportUrl();
        bootstrap.bind(serverConfig.getServerPort()).sync();
        IS_STARTED = true;
    }

    public void initServerConfig() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        ServerConfig serverConfig = PropertiesBootstrap.loadServerConfigFromLocal();
        this.setServerConfig(serverConfig);
        SERVER_CONFIG = serverConfig;
        // 初始化线程池和队列
        SERVER_CHANNEL_DISPATCHER.init(SERVER_CONFIG.getServerQueueSize(), SERVER_CONFIG.getServerBizThreadNums());
        // 初始化序列化框架 多选一
        String serverSerialize = serverConfig.getServerSerialize();
        EXTENSION_LOADER.loadExtension(SerializeFactory.class);
        LinkedHashMap<String, Class> serializeFactoryClassMap = EXTENSION_LOADER_CLASS_CACHE.get(SerializeFactory.class.getName());
        Class serializeFactoryClass = serializeFactoryClassMap.get(serverSerialize);
        if (serializeFactoryClass == null) {
            throw new RuntimeException("no match serialize type for " + serverSerialize);
        }
        SERVER_SERIALIZE_FACTORY = (SerializeFactory) serializeFactoryClass.newInstance();
        // 初始化过滤链 全部添加
        EXTENSION_LOADER.loadExtension(IServerFilter.class);
        LinkedHashMap<String, Class> iServerFilterClassMap = EXTENSION_LOADER_CLASS_CACHE.get(IServerFilter.class);
        ServerFilterChain serverFilterChain = new ServerFilterChain();
        for (String iServerFilterKey : iServerFilterClassMap.keySet()) {
            Class iServerFilterClass = iServerFilterClassMap.get(iServerFilterKey);
            if (iServerFilterClass == null) {
                throw new RuntimeException("no match iServerFilter type for " + iServerFilterKey);
            }
            serverFilterChain.addServerFilter((IServerFilter) iServerFilterClass.newInstance());
        }
        SERVER_FILTER_CHAIN = serverFilterChain;
    }

    public void exportService(ServiceWrapper serviceWrapper) {
        Object serviceBean = serviceWrapper.getServiceObj();
        if (serviceBean.getClass().getInterfaces().length == 0) {
            throw new RuntimeException("service must had interfaces!");
        }
        Class[] classes = serviceBean.getClass().getInterfaces();
        if (classes.length > 1) {
            throw new RuntimeException("service must only had one interfaces!");
        }
        if (REGISTRY_SERVICE == null) {
            try {
                EXTENSION_LOADER.loadExtension(RegistryService.class);
                LinkedHashMap<String, Class> registryClassMap = EXTENSION_LOADER_CLASS_CACHE.get(RegistryService.class.getName());
                Class registryClass = registryClassMap.get(serverConfig.getRegisterType());
                REGISTRY_SERVICE = (RegistryService) registryClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("registryServiceType unKnow,error is ", e);
            }
        }
        Class interfaceClass = classes[0];
        PROVIDER_CLASS_MAP.put(interfaceClass.getName(), serviceBean);
        URL url = new URL();
        url.setServiceName(interfaceClass.getName());
        url.setApplicationName(serverConfig.getApplicationName());
        url.addParameter("host", CommonUtils.getIpAddress());
        url.addParameter("port", String.valueOf(serverConfig.getServerPort()));
        url.addParameter("group", String.valueOf(serviceWrapper.getGroup()));
        url.addParameter("limit", String.valueOf(serviceWrapper.getLimit()));
        PROVIDER_URL_SET.add(url);
        if (CommonUtils.isNotEmpty(serviceWrapper.getServiceToken())) {
            PROVIDER_SERVICE_WRAPPER_MAP.put(interfaceClass.getName(), serviceWrapper);
        }
    }

    private void batchExportUrl() {
        Thread task = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for (URL url : PROVIDER_URL_SET) {
                    REGISTRY_SERVICE.register(url);
                }
            }
        });
        task.start();
    }

    //public void registryService(Object serviceBean) {
    //    Class<?>[] interfaces = serviceBean.getClass().getInterfaces();
    //    if (interfaces.length == 0) {
    //        throw new RuntimeException("service must had interfaces!");
    //    }
    //    if (interfaces.length > 1) {
    //        throw new RuntimeException("service must only had one interfaces!");
    //    }
    //    Class interfaceClass = interfaces[0];
    //    PROVIDER_CLASS_MAP.put(interfaceClass.getName(), serviceBean);
    //}

    public static void main(String[] args) throws InterruptedException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        Server server = new Server();
        server.initServerConfig();
        iRpcListenerLoader = new IRpcListenerLoader();
        iRpcListenerLoader.init();
        ServiceWrapper dataServiceWrapper = new ServiceWrapper(new DataServiceImpl());
        dataServiceWrapper.setServiceToken("token-a");
        dataServiceWrapper.setLimit(2);
        ServiceWrapper userServiceWrapper = new ServiceWrapper(new UserServiceImpl());
        userServiceWrapper.setServiceToken("token-b");
        userServiceWrapper.setLimit(2);
        server.exportService(dataServiceWrapper);
        server.exportService(userServiceWrapper);
        ApplicationShutdownHook.registryShutdownHook();
        server.startApplication();
    }
}
