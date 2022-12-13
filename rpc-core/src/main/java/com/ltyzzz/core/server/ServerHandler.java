package com.ltyzzz.core.server;

import com.ltyzzz.core.common.RpcProtocol;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import static com.ltyzzz.core.cache.CommonServerCache.*;

public class ServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        // 将收到的msg转为RpcProtocol自定义类
        RpcProtocol rpcProtocol = (RpcProtocol) msg;
        ServerChannelReadData serverChannelReadData = new ServerChannelReadData();
        serverChannelReadData.setRpcProtocol(rpcProtocol);
        serverChannelReadData.setChannelHandlerContext(ctx);
        SERVER_CHANNEL_DISPATCHER.add(serverChannelReadData);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        Channel channel = ctx.channel();
        if (channel.isActive()) {
            ctx.close();
        }
    }
}
