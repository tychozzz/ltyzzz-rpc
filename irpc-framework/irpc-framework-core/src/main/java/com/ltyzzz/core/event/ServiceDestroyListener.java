package com.ltyzzz.core.event;

import com.ltyzzz.core.registry.URL;

import static com.ltyzzz.core.cache.CommonServerCache.PROVIDER_URL_SET;
import static com.ltyzzz.core.cache.CommonServerCache.REGISTRY_SERVICE;

public class ServiceDestroyListener implements IRpcListener<IRpcDestroyEvent> {

    @Override
    public void callBack(Object t) {
        for (URL url : PROVIDER_URL_SET) {
            REGISTRY_SERVICE.unRegister(url);
        }
    }
}
