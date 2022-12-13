package com.ltyzzz.core.filter.server;

import com.ltyzzz.core.annotations.SPI;
import com.ltyzzz.core.common.RpcInvocation;
import com.ltyzzz.core.filter.IServerFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SPI("before")
public class ServerLogFilterImpl implements IServerFilter {

    private static Logger logger = LoggerFactory.getLogger(ServerLogFilterImpl.class);

    @Override
    public void doFilter(RpcInvocation rpcInvocation) {
        logger.info(rpcInvocation.getAttachments().get("c_app_name") + " do invoke -----> " + rpcInvocation.getTargetServiceName() + "#" + rpcInvocation.getTargetMethod());
    }
}
