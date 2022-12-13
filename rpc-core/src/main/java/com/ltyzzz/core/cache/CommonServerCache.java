package com.ltyzzz.core.cache;

import com.ltyzzz.core.common.ServerServiceSemaphoreWrapper;
import com.ltyzzz.core.config.ServerConfig;
import com.ltyzzz.core.dispatcher.ServerChannelDispatcher;
import com.ltyzzz.core.filter.server.ServerAfterFilterChain;
import com.ltyzzz.core.filter.server.ServerBeforeFilterChain;
import com.ltyzzz.core.registry.RegistryService;
import com.ltyzzz.core.registry.URL;
import com.ltyzzz.core.registry.zookeeper.AbstractRegister;
import com.ltyzzz.core.serialize.SerializeFactory;
import com.ltyzzz.core.server.ServiceWrapper;
import io.netty.util.internal.ConcurrentSet;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CommonServerCache {

    public static final Map<String,Object> PROVIDER_CLASS_MAP = new ConcurrentHashMap<>();
    public static final Set<URL> PROVIDER_URL_SET = new ConcurrentSet<>();
    public static AbstractRegister REGISTRY_SERVICE;
    public static SerializeFactory SERVER_SERIALIZE_FACTORY;
    public static ServerConfig SERVER_CONFIG;
    public static ServerBeforeFilterChain SERVER_BEFORE_FILTER_CHAIN;
    public static ServerAfterFilterChain SERVER_AFTER_FILTER_CHAIN;
    public static final Map<String, ServiceWrapper> PROVIDER_SERVICE_WRAPPER_MAP = new ConcurrentHashMap<>();
    public static Boolean IS_STARTED = false;
    public static ServerChannelDispatcher SERVER_CHANNEL_DISPATCHER = new ServerChannelDispatcher();
    public static final Map<String, ServerServiceSemaphoreWrapper> SERVER_SERVICE_SEMAPHORE_MAP = new ConcurrentHashMap<>(64);
}
