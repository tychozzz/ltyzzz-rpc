package com.ltyzzz.core.server;

import com.ltyzzz.core.common.RpcDecoder;
import com.ltyzzz.core.common.RpcEncoder;
import com.ltyzzz.core.common.config.ServerConfig;
import com.ltyzzz.core.service.DataServiceImpl;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import static com.ltyzzz.core.common.cache.CommonServerCache.PROVIDER_CLASS_MAP;

public class Server {

    private static EventLoopGroup bossGroup = null;
    private static EventLoopGroup workerGroup = null;
    private ServerConfig serverConfig;

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
        bootstrap.bind(serverConfig.getPort()).sync();
    }

    public void registryService(Object serviceBean) {
        Class<?>[] interfaces = serviceBean.getClass().getInterfaces();
        if (interfaces.length == 0) {
            throw new RuntimeException("service must had interfaces!");
        }
        if (interfaces.length > 1) {
            throw new RuntimeException("service must only had one interfaces!");
        }
        Class interfaceClass = interfaces[0];
        PROVIDER_CLASS_MAP.put(interfaceClass.getName(), serviceBean);
    }

    public static void main(String[] args) throws InterruptedException {
        Server server = new Server();
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(9090);
        server.setServerConfig(serverConfig);
        server.registryService(new DataServiceImpl());
        server.startApplication();
    }
}
