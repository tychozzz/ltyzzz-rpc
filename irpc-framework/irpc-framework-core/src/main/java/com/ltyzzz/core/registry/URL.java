package com.ltyzzz.core.registry;

import com.ltyzzz.core.registry.zookeeper.ProviderNodeInfo;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class URL {

    private String applicationName;

    private String serviceName;

    private Map<String, String> parameters = new HashMap<>();

    public void addParameter(String key, String value) {
        parameters.putIfAbsent(key, value);
    }

    public static String buildProviderUrlStr(URL url) {
        String host = url.getParameters().get("host");
        String port = url.getParameters().get("port");
        return new String((url.getApplicationName() + ";" +
                url.getServiceName() + ";" +
                host + ":" + port + ";" +
                System.currentTimeMillis()).getBytes(),
                StandardCharsets.UTF_8
        );
    }

    public static String buildConsumerUrlStr(URL url) {
        String host = url.getParameters().get("host");
        return new String((url.getApplicationName() + ";" +
                url.getServiceName() + ";" +
                host + ";" +
                System.currentTimeMillis()).getBytes(),
                StandardCharsets.UTF_8
        );
    }

    public static ProviderNodeInfo buildURLFromUrlStr(String providerNodeStr) {
        String[] items = providerNodeStr.split("/");
        ProviderNodeInfo info = new ProviderNodeInfo();
        info.setServiceName(items[2]);
        info.setAddress(items[4]);
        return info;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
}
