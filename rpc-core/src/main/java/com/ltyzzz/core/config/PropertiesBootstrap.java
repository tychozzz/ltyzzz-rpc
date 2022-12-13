package com.ltyzzz.core.config;

import java.io.IOException;

import static com.ltyzzz.core.constants.RpcConstants.*;

public class PropertiesBootstrap {

    private volatile boolean configIsReady;

    public static final String SERVER_PORT = "irpc.serverPort";
    public static final String REGISTER_ADDRESS = "irpc.registerAddr";
    public static final String REGISTER_TYPE = "irpc.registerType";
    public static final String APPLICATION_NAME = "irpc.applicationName";
    public static final String PROXY_TYPE = "irpc.proxyType";
    public static final String ROUTER_TYPE = "irpc.router";
    public static final String SERVER_SERIALIZE_TYPE = "irpc.serverSerialize";
    public static final String CLIENT_SERIALIZE_TYPE = "irpc.clientSerialize";
    public static final String CLIENT_DEFAULT_TIME_OUT = "irpc.client.default.timeout";
    public static final String SERVER_BIZ_THREAD_NUMS = "irpc.server.biz.thread.nums";
    public static final String SERVER_QUEUE_SIZE = "irpc.server.queue.size";
    public static final String SERVER_MAX_CONNECTION = "irpc.server.max.connection";
    public static final String SERVER_MAX_DATA_SIZE = "irpc.server.max.data.size";
    public static final String CLIENT_MAX_DATA_SIZE = "irpc.client.max.data.size";

    public static ServerConfig loadServerConfigFromLocal() {
        try {
            PropertiesLoader.loadConfiguration();
        } catch (IOException e) {
            throw new RuntimeException("loadServerConfigFromLocal fail,e is {}", e);
        }
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setServerPort(PropertiesLoader.getPropertiesIntegerDefault(SERVER_PORT, DEFAULT_SERVER_PORT));
        serverConfig.setApplicationName(PropertiesLoader.getPropertiesStrDefault(APPLICATION_NAME, DEFAULT_PROVIDER_APPLICATION_NAME));
        serverConfig.setRegisterAddr(PropertiesLoader.getPropertiesStrDefault(REGISTER_ADDRESS, DEFAULT_REGISTER_ADDR));
        serverConfig.setRegisterType(PropertiesLoader.getPropertiesStrDefault(REGISTER_TYPE, DEFAULT_REGISTER_TYPE));
        serverConfig.setServerSerialize(PropertiesLoader.getPropertiesStrDefault(SERVER_SERIALIZE_TYPE, JDK_SERIALIZE_TYPE));
        serverConfig.setServerBizThreadNums(PropertiesLoader.getPropertiesIntegerDefault(SERVER_BIZ_THREAD_NUMS,DEFAULT_THREAD_NUMS));
        serverConfig.setServerQueueSize(PropertiesLoader.getPropertiesIntegerDefault(SERVER_QUEUE_SIZE,DEFAULT_QUEUE_SIZE));
        serverConfig.setMaxConnections(PropertiesLoader.getPropertiesIntegerDefault(SERVER_MAX_CONNECTION,DEFAULT_MAX_CONNECTION_NUMS));
        serverConfig.setMaxServerRequestData(PropertiesLoader.getPropertiesIntegerDefault(SERVER_MAX_DATA_SIZE,SERVER_DEFAULT_MSG_LENGTH));
        return serverConfig;
    }

    public static ClientConfig loadClientConfigFromLocal(){
        try {
            PropertiesLoader.loadConfiguration();
        } catch (IOException e) {
            throw new RuntimeException("loadClientConfigFromLocal fail,e is {}", e);
        }
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setApplicationName(PropertiesLoader.getPropertiesStrDefault(APPLICATION_NAME, DEFAULT_CONSUMER_APPLICATION_NAME));
        clientConfig.setRegisterAddr(PropertiesLoader.getPropertiesStrDefault(REGISTER_ADDRESS, DEFAULT_REGISTER_ADDR));
        clientConfig.setRegisterType(PropertiesLoader.getPropertiesStrDefault(REGISTER_TYPE, DEFAULT_REGISTER_TYPE));
        clientConfig.setProxyType(PropertiesLoader.getPropertiesStrDefault(PROXY_TYPE,JDK_PROXY_TYPE));
        clientConfig.setRouterStrategy(PropertiesLoader.getPropertiesStrDefault(ROUTER_TYPE,RANDOM_ROUTER_TYPE));
        clientConfig.setClientSerialize(PropertiesLoader.getPropertiesStrDefault(CLIENT_SERIALIZE_TYPE, JDK_SERIALIZE_TYPE));
        clientConfig.setTimeOut(PropertiesLoader.getPropertiesIntegerDefault(CLIENT_DEFAULT_TIME_OUT,DEFAULT_TIMEOUT));
        clientConfig.setMaxServerRespDataSize(PropertiesLoader.getPropertiesIntegerDefault(CLIENT_MAX_DATA_SIZE,CLIENT_DEFAULT_MSG_LENGTH));
        return clientConfig;
    }


}
