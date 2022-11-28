package com.ltyzzz.core.server;

import com.ltyzzz.core.common.RpcDecoder;
import com.ltyzzz.core.common.RpcEncoder;
import com.ltyzzz.core.common.config.PropertiesBootstrap;
import com.ltyzzz.core.common.config.ServerConfig;
import com.ltyzzz.core.common.utils.CommonUtils;
import com.ltyzzz.core.event.IRpcListenerLoader;
import com.ltyzzz.core.registry.RegistryService;
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

import static com.ltyzzz.core.common.cache.CommonServerCache.*;

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
    }

    public void initServerConfig() {
        ServerConfig serverConfig = PropertiesBootstrap.loadServerConfigFromLocal();
        this.setServerConfig(serverConfig);
    }

    public void exportService(Object serviceBean) {
        Class<?>[] classes = serviceBean.getClass().getInterfaces();
        if (classes.length == 0) {
            throw new RuntimeException("service must had interfaces!");
        }
        if (classes.length > 1) {
            throw new RuntimeException("service must only had one interfaces!");
        }
        if (REGISTRY_SERVICE == null) {
            REGISTRY_SERVICE = new ZookeeperRegister(serverConfig.getRegisterAddr());
        }
        Class interfaceClass = classes[0];
        PROVIDER_CLASS_MAP.put(interfaceClass.getName(), serviceBean);
        URL url = new URL();
        url.setServiceName(interfaceClass.getName());
        url.setApplicationName(serverConfig.getApplicationName());
        url.addParameter("host", CommonUtils.getIpAddress());
        url.addParameter("port", String.valueOf(serverConfig.getServerPort()));
        PROVIDER_URL_SET.add(url);
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

    public static void main(String[] args) throws InterruptedException {
        Server server = new Server();
        server.initServerConfig();
        iRpcListenerLoader = new IRpcListenerLoader();
        iRpcListenerLoader.init();
        server.exportService(new DataServiceImpl());
        server.exportService(new UserServiceImpl());
        ApplicationShutdownHook.registryShutdownHook();
        server.startApplication();
    }
}
