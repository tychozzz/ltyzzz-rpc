package com.ltyzzz.core.event;

import com.ltyzzz.core.common.ChannelFutureWrapper;
import com.ltyzzz.core.registry.URL;
import com.ltyzzz.core.registry.zookeeper.ProviderNodeInfo;

import java.util.List;

import static com.ltyzzz.core.cache.CommonClientCache.CONNECT_MAP;
import static com.ltyzzz.core.cache.CommonClientCache.IROUTER;

public class ProviderNodeDataChangeListener implements IRpcListener<IRpcNodeChangeEvent> {

    @Override
    public void callBack(Object t) {
        ProviderNodeInfo providerNodeInfo = ((ProviderNodeInfo) t);
        List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(providerNodeInfo.getServiceName());
        for (ChannelFutureWrapper channelFutureWrapper : channelFutureWrappers) {
            // 重置分组信息
            String address = channelFutureWrapper.getHost() + ":" + channelFutureWrapper.getPort();
            if (address.equals(providerNodeInfo.getAddress())) {
                channelFutureWrapper.setGroup(providerNodeInfo.getGroup());
                // 修改权重
                channelFutureWrapper.setWeight(providerNodeInfo.getWeight());
                URL url = new URL();
                url.setServiceName(providerNodeInfo.getServiceName());
                // 更新权重
                IROUTER.updateWeight(url);
                break;
            }
        }
    }
}
