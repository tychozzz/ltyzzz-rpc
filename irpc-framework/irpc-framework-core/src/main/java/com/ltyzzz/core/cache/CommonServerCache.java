package com.ltyzzz.core.cache;

import com.ltyzzz.core.config.ServerConfig;
import com.ltyzzz.core.filter.server.ServerFilterChain;
import com.ltyzzz.core.registry.RegistryService;
import com.ltyzzz.core.registry.URL;
import com.ltyzzz.core.serialize.SerializeFactory;
import com.ltyzzz.core.server.ServiceWrapper;
import io.netty.util.internal.ConcurrentSet;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CommonServerCache {

    public static final Map<String,Object> PROVIDER_CLASS_MAP = new ConcurrentHashMap<>();
    public static final Set<URL> PROVIDER_URL_SET = new ConcurrentSet<>();
    public static RegistryService REGISTRY_SERVICE;
    public static SerializeFactory SERVER_SERIALIZE_FACTORY;
    public static ServerConfig SERVER_CONFIG;
    public static ServerFilterChain SERVER_FILTER_CHAIN;
    public static final Map<String, ServiceWrapper> PROVIDER_SERVICE_WRAPPER_MAP = new ConcurrentHashMap<>();
}
