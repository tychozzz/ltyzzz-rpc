# RPC项目记录

## 1. 引入Proxy代理层

采用JDK类代理，执行的逻辑为：将请求方法放入SEND_QUEUE队列中，自旋等待结果响应结果（从RESP_MAP中取出）

关键代码如下：

```java
RESP_MAP.put(rpcInvocation.getUuid(), OBJECT);
SEND_QUEUE.add(rpcInvocation); // 放入阻塞队列中
// 可以设置超时时间
while (true) {
    Object object = RESP_MAP.get(rpcInvocation.getUuid());
    if (object instanceof RpcInvocation) {
        return ((RpcInvocation) object).getResponse();
    }
}
```

### 1.1 基本流程

1.   Client启动时会开启一个异步线程阻塞队列，等待接收代理类放入的RpcInvocation，并将其顺序发送给对应Server

     ```java
     asyncSendJob.start();
     // 异步线程 run代码：真正执行网络通信的操作
     RpcInvocation data = SEND_QUEUE.take(); // 阻塞等待接收代理类放入RpcInvocation
     String json = JSON.toJSONString(data);
     RpcProtocol rpcProtocol = new RpcProtocol(json.getBytes());
     ChannelFuture channelFuture = ConnectionHandler.getChannelFuture(data.getTargetServiceName());
     channelFuture.channel().writeAndFlush(rpcProtocol);
     ```

2.   Client先通过代理类为RpcInvocation（RpcProtocol中content的具体实现）设置必要的参数，

     -   如：目标服务、目标方法、参数、UUID等，其中UUID是为了保证Client接收结果时判断一致

     代理类还有如下几点核心操作：

     -   将该uuid放入一个结果集map中，key为uuid，value为NULL对象
     -   将封装好的RpcInvocation类放入阻塞队列中
     -   最后代理类开始自旋一定时间，从结果集map中通过uuid获取其value：RpcInvocation，从中获取response结果

3.   异步线程阻塞队列阻塞式地获取到RpcInvocation后，将其再次封装为RpcProtocol（包含有magicNumber、content、contentLength），经过Encoder编码后发送给Server

4.   Server收到后进行Decode解码，ServerHandler将解码后的结果转为RpcProtocol，并获取其content，将content再转为RpcInvocation类。从该类中获取对应的目标服务属性，通过该属性从map（专门用来保存已经注册的服务信息）中找到对应服务，再通过目标方法属性从服务中找到对应的方法，并invoke执行得到返回结果。

     注意，之前传递的RpcInvocation类的response为空，为它set返回结果。

     最后将完整的RpcInvocation再次封装为RpcProtocol类并通过Encoder编码发送给Client

5.   Client通过Decoder将数据包解码，经由ClientHandler将解码后的结果转为RpcProtocol，再将其cotent转为RpcInvocation，通过之前的结果集map判断请求与响应是否一致。若一致，则将其放入结果集map，此时自旋等待的代理类便可从中取到RpcInvocation，并返回给Client。

## 2. 引入Zookeeper注册中心

Zookeeper节点

```
/irpc/com.ltyzzz.core.service.DataService/provider/10.249.19.183:9093
```

添加Zookeeper注册中心后

### 2.1 Server端实现

Server main代码

```java
Server server = new Server();
server.initServerConfig(); // 初始化当前服务提供者的基本信息
server.exportService(new DataServiceImpl()); // 暴露所提供的服务接口
server.startApplication(); // 启动服务端
```

-   在exportService方法中，将服务添加map中，服务提供者添加到set中

    URL类是配置类，基于其进行存储

    ```java
    PROVIDER_CLASS_MAP.put(interfaceClass.getName(), serviceBean);
    URL url = new URL();
    url.setServiceName(interfaceClass.getName());
    url.setApplicationName(serverConfig.getApplicationName());
    url.addParameter("host", CommonUtils.getIpAddress());
    url.addParameter("port", String.valueOf(serverConfig.getServerPort()));
    PROVIDER_URL_SET.add(url); 
    ```

-   在startApplication方法中，调用batchExportUrl方法，开启异步任务，从PROVIDER_URL_SET中获取URL，进行服务注册

    其中registerService由ZookeeperRegister实现

    ```java
    registryService.register(url); // 注册该服务 -> 本质是在Zookeeper中建立相应的节点
    // register方法中除了建立节点，还需要将URL添加到PROVIDER_URL_SET中
    // -> PROVIDER_URL_SET.add(url);
    ```

### 2.2 Client端实现

Client main代码

```java
Client client = new Client();
RpcReference rpcReference = client.initClientApplication(); //RpcReference用于实现JDK代理
DataService dataService = rpcReference.getProxy(DataService.class);
client.doSubscribeService(DataService.class); // 订阅DataService类
ConnectionHandler.setBootstrap(client.getBootstrap()); // 为ConnectionHandler设置bootstrap，具体参照2.4
client.doConnectServer(); // 建立连接 -> 本质是
client.startClient(); // 开启异步任务，进行网络通信，具体操作仍为阻塞队列逻辑
```

-   在initClientApplication方法中，除了进行Bootstrap等与Netty相关的初始化操作外，还进行了事件监听器的初始化

    在init方法中，向iRpcListenerLoader中添加了ServiceUpdateListener监听器

    监听事件参照 `2.3`

    ```java
    iRpcListenerLoader = new IRpcListenerLoader();
    iRpcListenerLoader.init();
    ```

-   在doSubscribeService方法中，初始化ZookeeperRegister，定义URL。根据此URL订阅相应的服务

    ```java
    URL url = new URL();
    url.setApplicationName(clientConfig.getApplicationName());
    url.setServiceName(serviceBean.getName());
    url.addParameter("host", CommonUtils.getIpAddress());
    abstractRegister.subscribe(url); // 订阅该服务 -> 本质是在Zookeeper中建立相应的节点
    // register方法中除了建立节点，还需要将URL添加到SUBSCRIBE_SERVICE_LIST中
    // -> SUBSCRIBE_SERVICE_LIST.add(url.getServiceName());
    ```

-   在doConnectServer方法中，提前与所有已注册的服务建立连接，并监听这些服务的变化（上线、下线、更改等）

    1.   监听事件参照 `2.3`
    2.   ConnectionHandler建立连接逻辑参照 `2.4`

    ```java
    for (String providerServiceName : SUBSCRIBE_SERVICE_LIST) {
        // getProviderIps方法获得形如「10.249.19.183:9093」此类的IP地址列表
        List<String> providerIps = abstractRegister.getProviderIps(providerServiceName);
        for (String providerIp : providerIps) {
            try {
                // connect方法作用为往CONNECT_MAP中放相应的连接
                ConnectionHandler.connect(providerServiceName, providerIp);
            } catch (InterruptedException e) {
                logger.error("[doConnectServer] connect fail ", e);
            }
        }
        URL url = new URL();
        url.setServiceName(providerServiceName);
        // 开启Watcher监听事件
        abstractRegister.doAfterSubscribe(url);
    }
    ```

### 2.3 监听事件机制实现

订阅之后开启监听事件，主要用于监听已注册服务的变化

1.   IRpcListenerLoader：用于注册与管理监听器。当事件发生时，调用相应的监听器回调方法

     IRpcEvent为发生事件接口，IRpcListener为事件监听器接口

     ```java
     private static List<IRpcListener> iRpcListenerList = new ArrayList<>();
     private static ExecutorService eventThreadPool = Executors.newFixedThreadPool(2); // 执行回调callBack方法
     ```

     ```
     ├── registerListener(IRpcListener iRpcListener) // 注册监听器事件
     ├── sendEvent(IRpcEvent iRpcEvent) // 调用监听器对应回调方法
     ```

     sendEvent方法实现如下

     ```java
     for (IRpcListener<?> iRpcListener : iRpcListenerList) {
         // 获取listener上监听事件的泛型
         Class<?> type = getInterfaceT(iRpcListener);
         // 判断Listener监听事件的泛型是否与Watcher传递的一致
         if(type.equals(iRpcEvent.getClass())){
             eventThreadPool.execute(new Runnable() {
                 @Override
                 public void run() {
                     try {
                         // 一致则异步回调处理
                         iRpcListener.callBack(iRpcEvent.getData());
                     }catch (Exception e){
                         e.printStackTrace();
                     }
                 }
             });
         }
     }
     ```

2.   主要监听逻辑位于ZookeeperRegister中的watchChildNodeData方法，如下：

     当监听的Zookeeper服务Node发生变化时，触发Watcher事件，Watcher内调用ListenerLoader方法（事件为方法参数），由ListenerLoader寻找对应的Listener（通过传入的事件与Listener泛型上的事件对比）。

     -   URLChangeWrapper对应为发生变化的URL包装类：包括serviceName与providerUrlList

     ```java
     @Override
     public void process(WatchedEvent watchedEvent) {
         System.out.println(watchedEvent);
         String path = watchedEvent.getPath();
         List<String> childrenData = zkClient.getChildrenData(path);
         URLChangeWrapper urlChangeWrapper = new URLChangeWrapper();
         urlChangeWrapper.setProviderUrl(childrenData);
         urlChangeWrapper.setServiceName(path.split("/")[2]);
         IRpcEvent iRpcEvent = new IRpcUpdateEvent(urlChangeWrapper);
         IRpcListenerLoader.sendEvent(iRpcEvent);
     	// 继续循环监听
         watchChildNodeData(path);
     }
     ```

### 2.4 ConnectionHandler实现

按照单一职责的设计原则，将与连接有关的功能都统一封装在了一起。

主要用于Netty在客户端与服务端之间建立连接、断开连接、按照服务名获取连接等操作。

建立连接逻辑如下：

```java
// 形如 10.249.19.183:9093
String[] providerAddress = providerIp.split(":");
String ip = providerAddress[0];
Integer port = Integer.valueOf(providerAddress[1]);
// 关键代码：创建ChannelFuture，即与目的服务简历底层通信连接
ChannelFuture channelFuture = bootstrap.connect(ip, port).sync();
// 连接包装类对象
ChannelFutureWrapper wrapper = new ChannelFutureWrapper();
wrapper.setChannelFuture(channelFuture);
wrapper.setHost(ip);
wrapper.setPort(port);
SERVER_ADDRESS.add(providerIp);
List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(providerServiceName);
if (CommonUtils.isEmptyList(channelFutureWrappers)) {
    channelFutureWrappers = new ArrayList<>();
}
channelFutureWrappers.add(wrapper);
// 将连接添加到CONNECT_MAP中
// 连接CONNECT_MAP -> key：需要调用的serviceName
//				  -> value：与多个服务提供者建立的连接，为List
CONNECT_MAP.put(providerServiceName, channelFutureWrappers);
```

### 2.5 远程调用流程


```
├── client
│   ├── Client.java                       // 客户端启动类
│   ├── ClientHandler.java				  //
│   ├── ConnectionHandler.java            //
│   └── RpcReference.java                 //
├── common
│   ├── ChannelFutureWrapper.java         //
│   ├── RpcDecoder.java                   //
│   ├── RpcEncoder.java                   //
│   ├── RpcInvocation.java                //
│   ├── RpcProtocol.java                  //
│   ├── cache
│   │   ├── CommonClientCache.java
│   │   └── CommonServerCache.java
│   ├── config
│   │   ├── ClientConfig.java
│   │   ├── PropertiesBootstrap.java
│   │   ├── PropertiesLoader.java
│   │   └── ServerConfig.java
│   ├── constants
│   │   └── RpcConstants.java
│   └── utils
│       └── CommonUtils.java
├── event
│   ├── IRpcEvent.java
│   ├── IRpcListener.java
│   ├── IRpcListenerLoader.java
│   ├── IRpcUpdateEvent.java
│   ├── ServiceUpdateListener.java
│   └── URLChangeWrapper.java
├── proxy
│   ├── JDKClientInvocationHandler.java
│   ├── JDKProxyFactory.java
│   └── ProxyFactory.java
├── registry
│   ├── RegistryService.java
│   ├── URL.java
│   └── zookeeper
│       ├── AbstractRegister.java
│       ├── AbstractZookeeperClient.java
│       ├── CuratorZookeeperClient.java
│       ├── ProviderNodeInfo.java
│       └── ZookeeperRegister.java
├── server
│   ├── Server.java
│   └── ServerHandler.java
└── service
    ├── DataService.java
    └── DataServiceImpl.java
```

