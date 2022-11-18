package com.ltyzzz.core.client;

import com.alibaba.fastjson.JSON;
import com.ltyzzz.core.common.RpcInvocation;
import com.ltyzzz.core.common.RpcProtocol;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import static com.ltyzzz.core.common.cache.CommonClientCache.RESP_MAP;

public class ClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 此时的msg已经被解码，直接转为RpcProtocol对象即可
        RpcProtocol rpcProtocol = (RpcProtocol) msg;
        byte[] content = rpcProtocol.getContent();
        String json = new String(content, 0, content.length);
        // 将具体地content转为RpcInvocation
        RpcInvocation rpcInvocation = JSON.parseObject(json, RpcInvocation.class);
        if (!RESP_MAP.containsKey(rpcInvocation.getUuid())) {
            throw new IllegalArgumentException("server response is error!");
        }
        RESP_MAP.put(rpcInvocation.getUuid(), rpcInvocation);
        ReferenceCountUtil.release(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        Channel channel = ctx.channel();
        if(channel.isActive()){
            ctx.close();
        }
    }
}
