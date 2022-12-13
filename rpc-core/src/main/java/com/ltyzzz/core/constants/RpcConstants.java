package com.ltyzzz.core.constants;

public class RpcConstants {

    public static final short MAGIC_NUMBER = 19812;

    public static final String JDK_PROXY_TYPE = "jdk";

    public static final String JAVASSIST_PROXY_TYPE = "javassist";

    public static final String RANDOM_ROUTER_TYPE = "random";

    public static final String ROTATE_ROUTER_TYPE = "rotate";

    public static final String JDK_SERIALIZE_TYPE = "jdk";

    public static final String FAST_JSON_SERIALIZE_TYPE = "fastJson";

    public static final String HESSIAN2_SERIALIZE_TYPE = "hessian2";

    public static final String KRYO_SERIALIZE_TYPE = "kryo";

    public static final String DEFAULT_PROVIDER_APPLICATION_NAME = "irpc_provider";

    public static final String DEFAULT_CONSUMER_APPLICATION_NAME = "ircp_consumer";

    public static final String DEFAULT_REGISTER_ADDR = "localhost:2181";

    public static final String DEFAULT_REGISTER_TYPE = "zookeeper";

    public static final Integer DEFAULT_SERVER_PORT = 9093;

    public static final Integer DEFAULT_TIMEOUT = 3000;

    public static final Integer DEFAULT_THREAD_NUMS = 256;

    public static final Integer DEFAULT_QUEUE_SIZE = 512;

    public static final Integer DEFAULT_MAX_CONNECTION_NUMS = DEFAULT_THREAD_NUMS + DEFAULT_QUEUE_SIZE;

    public static final String DEFAULT_DECODE_CHAR = "$_i0#Xsop1_$";

    public static final int SERVER_DEFAULT_MSG_LENGTH = 1024 * 10;

    public static final int CLIENT_DEFAULT_MSG_LENGTH = 1024 * 10;
}
