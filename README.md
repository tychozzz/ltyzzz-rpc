# RPC项目总体流程

![rpc-process1](https://lty-image-bed.oss-cn-shenzhen.aliyuncs.com/blog/rpc-process1.png)

# RPC项目树

## 模块树

```
├── rpc-consumer
│   ├── rpc-consumer-demo          -> 未接入spring：consumer测试类
│   └── rpc-consumer-spring        -> 接入spring：consumer本地服务接口
├── rpc-core                       -> rpc核心实现逻辑模块
├── rpc-interface                  -> 远程服务接口
├── rpc-provider
│   ├── rpc-provider-demo          -> 未接入spring：provider测试类
│   ├── rpc-provider-goods         -> 接入spring：provider远程服务
│   ├── rpc-provider-pay           -> 接入spring：provider远程服务
│   └── rpc-provider-user          -> 接入spring：provider远程服务
├── rpc-spring-starter             -> spring-starter接入类
└── simple-rpc                     -> 简易rpc
```

## 核心模块树

```
├── annotations                    -> 项目注解包
├── cache                          -> 项目全局缓存
├── client                         -> 客户端相关类（请求处理、启动加载）
├── common                         -> 通用模块
├── config                         -> 项目配置（服务端、客户端属性配置）
├── constants                      -> 项目常量
├── dispatcher                     -> 服务端请求解耦
├── event                          -> 事件监听机制
├── exception                      -> 全局异常
├── filter                         -> 责任链模式过滤请求
├── proxy                          -> 动态代理
├── registry                       -> 注册中心
├── router                         -> 路由选择负载均衡
├── serialize                      -> 序列化与反序列化
├── server                         -> 服务端相关类（请求处理、启动加载）
├── service                        -> 测试服务接口
├── spi                            -> SPI自定义加载类
└── utils                          -> 项目工具包
```

# RPC项目测试

## 普通测试

1.   进入rpc-provider/rpc-provider-demo模块下，运行ProviderDemo主方法

     <img src="https://lty-image-bed.oss-cn-shenzhen.aliyuncs.com/blog/image-20221214155224257.png" alt="image-20221214155224257" style="zoom:50%;" />

2.   进入rpc-consumer/rpc-consumer-demo模块下，运行ConsumerDemo主方法

     <img src="https://lty-image-bed.oss-cn-shenzhen.aliyuncs.com/blog/image-20221214155301390.png" alt="image-20221214155301390" style="zoom:50%;" />

## 接入Springboot测试

1.   进入rpc-provider模块下，分别运行rpc-provider-goods、rpc-provider-pay、rpc-provider-user三个服务启动类

     <img src="https://lty-image-bed.oss-cn-shenzhen.aliyuncs.com/blog/image-20221214155517934.png" alt="image-20221214155517934" style="zoom: 50%;" />

     2.   进入rpc-consumer/rpc-consumer-spring模块下，运行服务启动类

          <img src="https://lty-image-bed.oss-cn-shenzhen.aliyuncs.com/blog/image-20221214155636798.png" alt="image-20221214155636798" style="zoom:50%;" />

     3.   Consumer默认端口为8081，在浏览器中输入 http://localhost:8081/api-test/do-test 进行远程服务调用基本测试

## 自定义配置

在项目模块的resouces文件下，有 `irpc.properties` 文件，用于配置Consumer（服务消费者）与Provider（服务提供者）的基本属性

1.   Consumer基本配置

     ```properties
     # 应用名
     irpc.applicationName=irpc-consumer
     # 注册中心地址
     irpc.registerAddr=localhost:2181
     # 注册中心类型
     irpc.registerType=zookeeper
     # 动态代理类型
     irpc.proxyType=javassist
     # 路由策略类型
     irpc.router=rotate
     # 序列化类型
     irpc.clientSerialize=jdk
     # 请求超时时间
     irpc.client.default.timeout=3000
     # 最大发送数据包
     irpc.client.max.data.size=4096
     ```

2.   Provider基本配置

     ```properties
     # 服务提供者端口号
     irpc.serverPort=9021
     # 服务提供者名称
     irpc.applicationName=good-provider
     # 注册中心地址
     irpc.registerAddr=localhost:2181
     # 注册中心类型
     irpc.registerType=zookeeper
     # 序列化类型
     irpc.serverSerialize=fastJson
     # 服务端异步处理队列大小
     irpc.server.queue.size=513
     # 服务端线程池大小
     irpc.server.biz.thread.nums=257
     # 服务端最大连接数
     irpc.server.max.connection=100
     # 服务端可接收数据包最大值
     irpc.server.max.data.size=4096
     ```

# RPC项目介绍

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

1.   建立连接逻辑如下：connect方法

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

2.   获取连接逻辑如下：getChannelFuture方法

     每个服务可以有多个服务提供者（对应于多个物理机器）

     负载均衡策略：采用简单的random函数随机选取

     ```java
     List<ChannelFutureWrapper> channelFutureWrappers = CONNECT_MAP.get(providerServiceName);
     if (CommonUtils.isEmptyList(channelFutureWrappers)) {
         throw new RuntimeException("no provider exist for " + providerServiceName);
     }
     ChannelFuture channelFuture = channelFutureWrappers.get(new Random().nextInt(channelFutureWrappers.size())).getChannelFuture();
     return channelFuture;
     ```

### 2.5 远程调用流程

## 3. 引入路由层

同一个服务可能对应着多个服务提供者，因此当客户端请求服务时，需要通过负载均衡策略从中选择一个合适的服务提供者

之前的设计思路为：从多个**连接**（ChannelFuture通道）中随机选择一个，进行网络通信

```java
ChannelFuture channelFuture = channelFutureWrappers.get(new Random().nextInt(channelFutureWrappers.size())).getChannelFuture();
```

引入路由层，可以自定义负载均衡策略进行优化。

基于 `SERVICE_ROUTER_MAP` 实现

-   key为服务提供者名字，value为对应的连接数组

```
key -> ProviderServiceName: String
value -> ChannelFutureWrapper[]: Array
```

### 3.1 带权重的随机选取策略

自定义随机选取逻辑，将转化后的连接数组存入 SERVICE_ROUTER_MAP 中

虽然是随机选取，但是权重值越大，被选取的次数也会越多

默认初始情况下weight值为100

### 3.2 轮询策略

直接按照添加的先后顺序获取连接，将转化后的连接数组存入 SERVICE_ROUTER_MAP 中

### 3.3 获取连接实现

ChannelFuturePollingRef为实现类，用于从SERVICE_ROUTER_MAP中根据服务提供者名字轮询获取连接

本质是通过原子类取模运算获取连接

```java
private AtomicLong referenceTimes = new AtomicLong(0);

public ChannelFutureWrapper getChannelFutureWrapper(String serviceName) {
    ChannelFutureWrapper[] arr = SERVICE_ROUTER_MAP.get(serviceName);
    long i = referenceTimes.getAndIncrement();
    int index = (int) (i % arr.length);
    return arr[index];
}
```

### 3.4 权重更新事件

每个服务提供者在注册服务时默认的权重初始值为100。当该值被修改后，触发权重更新事件，修改对应的 SERVICE_ROUTER_MAP

该更新事件也是通过Watcher与自定义的监听事件机制实现，参考 `2.3`

## 4. 整合序列化

引入多种序列化策略，由用户自行配置与选择对应的策略

-   FastJson
-   Hessian
-   Kryo
-   JDK自带的序列化

### 4.1 序列化工厂

创建序列化工厂接口，定义接口方法：serialize与deserialize（均为范型方法）

具体的序列化策略去实现该工厂类。

-   SerializeFactory
    -   FastJsonSerializeFactory
    -   HessianSerializeFactory
    -   KryoSerializeFactory
    -   JdkSerializeFactory

### 4.2 序列化策略配置

序列化策略在Server与Client初始化时从配置文件中加载

## 5. 引入责任链模式

### 5.1 责任链模式的意义

1.   对客户端请求进行鉴权

     客户端请求的远程接口可能需要进行权限校验（比如与用户隐私相关的数据），服务端必须确认该请求合法才可放行

2.   分组管理服务

     同一个服务可能存在多个分支，有的分支为dev代表正在处于开发阶段，有的分支为test代表正在处于测试阶段。

     为了避免客户端调用到正在开发中的服务，在进行远程调用时，需要根据group进行过滤。

3.   基于ip直连方式访问服务端

     可能存在两个名字相同但代码逻辑不同的服务。为了避免出现不同的结果，需要根据服务提供方的ip进行过滤

4.   调用过程中记录日志信息

传统模式中，客户端需要在发送请求之前，逐个的调用过滤请求的方法；服务端在接受请求之前，也需要逐个调用过滤请求的方法

这种模式下，代码耦合度高，且扩展性差。

而采用责任链模式可以带来：

-   发送者与接收方的处理对象类之间解耦。
-   封装每个处理对象，处理类的最小封装原则。
-   可以任意添加处理对象，调整处理对象之间的顺序，提高了维护性和可拓展性，可以根据需求新增处理类，满足开闭原则。
-   增强了对象职责指派的灵活性，当流程发生变化的时候，可以动态地改变链内的调动次序可动态的新增或者删除。
-   责任链简化了对象之间的连接。每个对象只需保持一个指向其后继者的引用，不需保持其他所有处理者的引用，这避免了使用众多的 if 或者 if···else 语句。
-   责任分担。每个类只需要处理自己该处理的工作，不该处理的传递给下一个对象完成，明确各类的责任范围，符合类的单一职责原则。

### 5.2 责任链设计

```
├── IFilter.java
├── IClientFilter.java                  -> 继承IFilter接口
├── IServerFilter.java				-> 继承IFilter接口
├── client
│   ├── ClientFilterChain.java		-> 客户端过滤链
│   ├── ClientLogFilterImpl.java        -> 日志过滤器实现类
│   ├── DirectInvokeFilterImpl.java     -> IP过滤器实现类
│   └── GroupFilterImpl.java            -> 分组过滤器实现类
└── server
    ├── ServerFilterChain.java		-> 服务器过滤链
    ├── ServerLogFilterImpl.java        -> 日志过滤器实现类
    └── ServerTokenFilterImpl.java      -> Token安全校验过滤器实现类

```

1.   首先创建IFilter接口，然后分别创建服务器与客户端对应的接口，继承IFilter接口
2.   分别创建服务器与客户端过滤链，用于存放过滤器实现类，并遍历过滤器实现类集合，执行过滤方法
3.   依次实现过滤器实现类

## 6. 可插拔式组件

### 6.1 SPI优势

使用Java SPI机制的优势是实现解耦，使得第三方服务模块的装配控制的逻辑与调用者的业务代码分离，而不是耦合在一起。应用程序可以根据实际业务情况启用框架扩展或替换框架组件。

相比使用提供接口jar包，供第三方服务模块实现接口的方式，SPI的方式使得源框架，不必关心接口的实现类的路径，可以不用通过下面的方式获取接口实现类：

-   代码硬编码import 导入实现类
-   指定类全路径反射获取：例如在JDBC4.0之前，JDBC中获取数据库驱动类需要通过**Class.forName("com.mysql.jdbc.Driver")**，类似语句先动态加载数据库相关的驱动，然后再进行获取连接等的操作
-   第三方服务模块把接口实现类实例注册到指定地方，源框架从该处访问实例

通过SPI的方式，第三方服务模块实现接口后，在第三方的项目代码的META-INF/services目录下的配置文件指定实现类的全路径名，源码框架即可找到实现类

### 6.2 SPI设计思路

设计一个SPI加载类，通过当前Class的类加载器去加载META-INF/irpc/目录底下存在的资源文件

在需要加载资源时（初始化序列化框架、初始化过滤链、初始化路由策略、初始化注册中心），使用SPI加载类去实现

从而避免了在代码中通过switch语句以硬编码的方式选择资源

## 7. 队列与多线程

### 7.1 串行同步阻塞问题

NIO线程常见的阻塞情况，一共两大类：

-   无意识：在ChannelHandler中编写了可能导致NIO线程阻塞的代码，但是用户没有意识到，包括但不限于查询各种数据存储器的操作、第三方服务的远程调用、中间件服务的调用、等待锁等。

-   有意识：用户知道有耗时逻辑需要额外处理，但是在处理过程中翻车了，比如主动切换耗时逻辑到业务线程池或者业务的消息队列做处理时发生阻塞，最典型的有对方是阻塞队列，锁竞争激烈导致耗时，或者投递异步任务给消息队列时异机房的网络耗时，或者任务队列满了导致等待，等等。

服务端接收到消息之后

1.   需要对消息进行解码，使字节序列变为消息对象。

2.   将消息对象与上下文传入ServerHandler中进行进一步处理。

     可能某个业务Handler处理流程非常耗时，如查询数据库。为了避免线程被长时间占用，采用异步消费进行处理

客户端通过动态代理层封装RpcInvocation对象并将其放入SEND_QUEUE队列后，需要同步阻塞等待最终处理的响应结果

-   可以将此处改为同步与异步两种方式

### 7.2 异步设计

1.   对于服务端：

     当请求抵达服务器时，将其直接丢入业务阻塞队列中，然后开辟一个新的线程，从阻塞队列中循环获取Handler请求任务。

     将获取到的任务对象交付于业务线程池进行消费处理。

2.   对于客户端：

     在RpcReferenceWrapper中设置一个isAsync字段，用于判断是否为异步。

     若该字段为True，则在动态代理层中，不需要同步阻塞等待响应结果，直接返回null即可。

## 8. 容错设计

### 8.1 报错日志打印

当客户端发送请求到指定的服务提供者后，其调用对应的方法，但此时方法出现异常Exception。

若将异常只记录在服务端中，则客户端较难定位异常发生的时间、位置与原因，因为同一个服务可能有多个服务提供者。

因此，服务端在处理异常时，需要将所有异常捕获，并写回到客户端。

实现流程如下：

1.   RpcInvocation类中添加异常字段

     ```java
     private Throwable e;
     ```

2.   服务端处理接收到的请求时，用try-catch进行捕获，并设置异常

     ```java
     // 业务异常
     rpcInvocation.setE(e);
     ```

3.   客户端处理器ClientHandler中，读取响应结果时，对异常进行判断。如果该字段不为空，则打印异常

     ```java
     if (rpcInvocation.getE() != null) {
         rpcInvocation.getE().printStackTrace();
     }
     ```

### 8.2 超时重试机制

反向代理在发送请求之后，会以异步或同步的方式等待结果返回。

因此在反向代理等待请求返回的过程中，可以对请求超时与否进行判断，并根据可重发次数进行重新发送。

### 8.3 服务端流量控制

#### 8.3.1 总体限流

限制服务端的总体连接数，超过指定连接数时，拒绝剩余的连接请求。

通过为ServerBootstrap设置最大连接数处理器，及时地对连接进行释放。

最大连接数在服务端的配置文件中配置。

```java
bootstrap.handler(new MaxConnectionLimitHandler(serverConfig.getMaxConnections()));
```

```java
Channel channel = (Channel) msg;
int conn = numConnection.incrementAndGet();
if (conn > 0 && conn <= maxConnectionNum) {
    this.childChannel.add(channel);
    channel.closeFuture().addListener(future -> {
        childChannel.remove(channel);
        numConnection.decrementAndGet();
    });
    super.channelRead(ctx, msg);
} else {
    numConnection.decrementAndGet();
    //避免产生大量的time_wait连接
    channel.config().setOption(ChannelOption.SO_LINGER, 0);
    channel.unsafe().closeForcibly();
    numDroppedConnections.increment();
    //这里加入一道cas可以减少一些并发请求的压力,定期地执行一些日志打印
    if (loggingScheduled.compareAndSet(false, true)) {
        ctx.executor().schedule(this::writeNumDroppedConnectionLog,1, TimeUnit.SECONDS);
    }
}
```

#### 8.3.2 单服务限流

采用 **Semaphore** 进行流量控制，在每一个服务进行注册时，便指定服务对应的最大连接数。

```java
//设置服务端的限流器
SERVER_SERVICE_SEMAPHORE_MAP.put(interfaceClass.getName(), new ServerServiceSemaphoreWrapper(serviceWrapper.getLimit()));
```

在请求到达服务端之前，配置一层前置过滤器。

-   当当前连接数超过最大连接数时，根据Semaphore的tryAcquire原理，会直接返回False，据此判断流量超峰，抛出异常。

```java
@Override
public void doFilter(RpcInvocation rpcInvocation) {
    String serviceName = rpcInvocation.getTargetServiceName();
    ServerServiceSemaphoreWrapper serverServiceSemaphoreWrapper = SERVER_SERVICE_SEMAPHORE_MAP.get(serviceName);
    //从缓存中提取semaphore对象
    Semaphore semaphore = serverServiceSemaphoreWrapper.getSemaphore();
    boolean tryResult = semaphore.tryAcquire();
    if (!tryResult) {
        LOGGER.error("[ServerServiceBeforeLimitFilterImpl] {}'s max request is {},reject now", rpcInvocation.getTargetServiceName(), serverServiceSemaphoreWrapper.getMaxNums());
        MaxServiceLimitRequestException iRpcException = new MaxServiceLimitRequestException(rpcInvocation);
        rpcInvocation.setE(iRpcException);
        throw iRpcException;
    }
}
```

当当前请求结束之后，需要对资源进行释放，也就是对Semaphore持有资源数加1。通过请求后置过滤器实现

```java
@Override
public void doFilter(RpcInvocation rpcInvocation) {
    String serviceName = rpcInvocation.getTargetServiceName();
    ServerServiceSemaphoreWrapper serverServiceSemaphoreWrapper = SERVER_SERVICE_SEMAPHORE_MAP.get(serviceName);
    serverServiceSemaphoreWrapper.getSemaphore().release();
}
```

## 9. 接入SpringBoot

### 9. 1 定义注解

1.   客户端对需要调用的服务添加 `@IRpcReference` 注解

     在Spring容器启动过程中，将带有此注解的字段进行构建，**让它们的句柄可以指向一个代理类**

     **这样在使用UserService和OrderService类对应的方法时候就会感觉到似乎在执行本地调用一样，降低开发者的代码编写难度。**

2.   服务端通过 `@IRpcService` 注解对服务进行暴露，将其注入到Spring容器中

     -   该注解内部添加了 `@Component` 注解，因此能被扫描到Spring容器中

### 9.2 定义自动装配对象类

#### 9.2.1 服务端

服务端自动装配流程

1.   初始化服务端配置
     -   从 `irpc.properties` 中读取相关配置并写入config
     -   初始化线程池、队列
     -   通过 `SPI` 初始化序列化框架、过滤链
     -   初始化并注册启动事件监听器

2.   Spring从容器中筛选出带有 `@IRpcService` 注解的类，以Map形式封装
3.   将每一个Map中的对象封装为 `ServiceWrapper` 对象，并从注解中提取并设置相应的属性，将service注册到注册中心
4.   开启服务端，准备接收任务

#### 9.2.2 客户端

客户端自动装配流程

1.   初始化客户端配置
     -   从 `irpc.properties` 中读取相关配置并写入config
     -   通过 `SPI` 初始化动态代理
2.   获取带有 `@IRpcReference` 注解的类，从注解中提取并设置相应的属性
3.   在注册中心中订阅对应的服务

## 附：额外记录

### 附1 本地公共缓存

缓存中主要存放客户端订阅信息、服务端注册信息、服务对应的通信连接信息、用于实现异步的队列等通用数据

分为客户端缓存与服务端缓存： CommonClientCache 与 CommonServerCache

### 附2 服务端终止监听事件

当某一服务提供者下线时，需要将其对应的服务器从Zookeeper注册中心中移除

监听机制原理相同，参考 `2.3`

回调方法逻辑如下：

```java
@Override
public void callBack(Object t) {
    for (URL url : PROVIDER_URL_SET) {
        REGISTRY_SERVICE.unRegister(url);
    }
}
```

### 附3 可扩展性设计

#### 冗余类

该RPC框架中很多地方存在着冗余设计，比如RpcReference、ApplicationShutdownHook、ChannelFuturePollingRef等

这些类中可能只有一个属性或一个方法，但是单独抽象成一个类，便于之后在此基础上进行扩展。

#### 事件监听解耦

Watcher对Zookeeper节点进行监听，当事件发生后，并不是直接处理，而是将该事件交于IRpcListenerLoader，让其选择对应对应的事件监听器去处理，进一步解耦。

因此，在初始化客户端与服务端的时候，需要将事件监听器注册到IRpcListenerLoader上进行管理。

### 附4 包装类设计

将同一业务逻辑下的属性进行封装，如：

-   URLChangeWrapper：当节点信息发生变化时，触发监听事件。

    将改变节点的路径与对应的服务名存放到该类中。当Watcher监听到节点信息变化时，便可将信息封装到该类中，发送给IRpcListenerLoader去处理

-   ChannelFutureWrapper：底层网络通信连接类，SERVICE_ROUTER_MAP存储的连接数组就是这个类

### 附5 配置类设计

将客户端与服务端的相关配置属性提取到配置文件中，避免硬编码，进一步解耦。

需要修改相关属性时，直接对配置文件进行修改。

基本流程为：服务端与客户端在启动时，通过配置方法从指定文件中读取配置项，将具体配置属性转化为配置对象。

结构树如下：

```
├── rpc-consumer
│   ├── ClientConfig.java               -> 客户端配置属性类
│   ├── PropertiesBootstrap.java        -> 用于设置配置并返回配置类对象	
│   ├── PropertiesLoader.java           -> 用于从文件中读取加载配置
│   └── ServerConfig.java               -> 服务端配置属性类
```

# RPC项目bug记录

1.   测试不引入springboot远程调用时，采用kryo在服务端无法完成反序列化

     解决方案：暂时更换为jdk序列化
