package com.ltyzzz.core.server;

import com.alibaba.fastjson.JSON;
import com.ltyzzz.core.common.RpcInvocation;
import com.ltyzzz.core.common.RpcProtocol;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.lang.reflect.Method;

import static com.ltyzzz.core.common.cache.CommonServerCache.PROVIDER_CLASS_MAP;

public class ServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 将收到的msg转为RpcProtocol自定义类
        RpcProtocol rpcProtocol = (RpcProtocol) msg;
        // 将msg的content内容进一步转换为RpcInvocation获得具体信息
        String json = new String(rpcProtocol.getContent(), 0, rpcProtocol.getContentLength());
        RpcInvocation rpcInvocation = JSON.parseObject(json, RpcInvocation.class);
        // 从服务提供者中获取目标服务
        Object aimObject = PROVIDER_CLASS_MAP.get(rpcInvocation.getTargetServiceName());
        // 获取目标服务的全部方法
        Method[] methods = aimObject.getClass().getDeclaredMethods();
        Object result = null;
        // 寻找得到对应的方法并执行得到方法结果
        for (Method method : methods) {
            if (method.getName().equals(rpcInvocation.getTargetMethod())) {
                if (method.getReturnType().equals(Void.TYPE)) {
                    method.invoke(aimObject, rpcInvocation.getArgs());
                } else {
                    result = method.invoke(aimObject, rpcInvocation.getArgs());
                }
                break;
            }
        }
        // 设置response
        rpcInvocation.setResponse(result);
        // 再次封装为RpcProtocol返回给客户端
        RpcProtocol respRpcProtocol = new RpcProtocol(JSON.toJSONString(rpcInvocation).getBytes());
        ctx.writeAndFlush(respRpcProtocol);
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
