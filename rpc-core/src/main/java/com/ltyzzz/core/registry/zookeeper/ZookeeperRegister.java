package com.ltyzzz.core.registry.zookeeper;

import com.alibaba.fastjson.JSON;
import com.ltyzzz.core.event.*;
import com.ltyzzz.core.registry.RegistryService;
import com.ltyzzz.core.registry.URL;
import com.ltyzzz.core.utils.CommonUtils;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ltyzzz.core.cache.CommonClientCache.CLIENT_CONFIG;
import static com.ltyzzz.core.cache.CommonServerCache.IS_STARTED;
import static com.ltyzzz.core.cache.CommonServerCache.SERVER_CONFIG;

public class ZookeeperRegister extends AbstractRegister implements RegistryService {

    private AbstractZookeeperClient zkClient;

    private String ROOT = "/irpc";

    public ZookeeperRegister() {
        String registryAddr = CLIENT_CONFIG != null ? CLIENT_CONFIG.getRegisterAddr() : SERVER_CONFIG.getRegisterAddr();
        this.zkClient = new CuratorZookeeperClient(registryAddr);
    }

    public ZookeeperRegister(String address) {
        this.zkClient = new CuratorZookeeperClient(address);
    }

    private String getProviderPath(URL url) {
        return ROOT + "/" + url.getServiceName() +
                "/provider/" + url.getParameters().get("host") +
                ":" + url.getParameters().get("port");
    }

    private String getConsumerPath(URL url) {
        return ROOT + "/" + url.getServiceName() +
                "/consumer/" + url.getApplicationName() +
                ":" + url.getParameters().get("host") + ":";
    }

    @Override
    public void register(URL url) {
        if (!zkClient.existNode(ROOT)) {
            zkClient.createPersistentData(ROOT, "");
        }
        String urlStr = URL.buildProviderUrlStr(url);
        if (zkClient.existNode(getProviderPath(url))) {
            zkClient.deleteNode(getProviderPath(url));
        }
        zkClient.createTemporaryData(getProviderPath(url), urlStr);
        super.register(url);
    }

    @Override
    public void unRegister(URL url) {
        if (!IS_STARTED) {
            return;
        }
        zkClient.deleteNode(getProviderPath(url));
        super.unRegister(url);
    }

    @Override
    public void subscribe(URL url) {
        if (!this.zkClient.existNode(ROOT)) {
            zkClient.createPersistentData(ROOT, "");
        }
        String urlStr = URL.buildConsumerUrlStr(url);
        if (zkClient.existNode(getConsumerPath(url))) {
            zkClient.deleteNode(getConsumerPath(url));
        }
        zkClient.createTemporarySeqData(getConsumerPath(url), urlStr);
        super.subscribe(url);
    }

    @Override
    public void unSubscribe(URL url) {
        zkClient.deleteNode(getConsumerPath(url));
        super.unSubscribe(url);
    }

    @Override
    public void doAfterSubscribe(URL url) {
        String servicePath = url.getParameters().get("servicePath");
        String newServerNodePath = ROOT + "/" + servicePath;
        this.watchChildNodeData(newServerNodePath);
        String providerIpStrJson = url.getParameters().get("providerIps");
        List<String> providerIpList = JSON.parseObject(providerIpStrJson, List.class);
        for (String providerIp : providerIpList) {
            this.watchNodeDataChange(ROOT + "/" + servicePath + "/" + providerIp);
        }
    }

    public void watchNodeDataChange(String newServerNodePath) {
        zkClient.watchChildNodeData(newServerNodePath, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                String path = watchedEvent.getPath();
                String nodeData = zkClient.getNodeData(path);
                ProviderNodeInfo providerNodeInfo = URL.buildURLFromUrlStr(nodeData);
                IRpcEvent iRpcEvent = new IRpcNodeChangeEvent(providerNodeInfo);
                IRpcListenerLoader.sendEvent(iRpcEvent);
                watchNodeDataChange(newServerNodePath);
            }
        });
    }

    public void watchChildNodeData(String newServerNodePath) {
        zkClient.watchChildNodeData(newServerNodePath, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                System.out.println(watchedEvent);
                String servicePath = watchedEvent.getPath();
                List<String> childrenDataList = zkClient.getChildrenData(servicePath);
                if (CommonUtils.isEmptyList(childrenDataList)) {
                    watchChildNodeData(servicePath);
                    return;
                }
                URLChangeWrapper urlChangeWrapper = new URLChangeWrapper();
                Map<String, String> nodeDetailInfoMap = new HashMap<>();
                for (String providerAddress : childrenDataList) {
                    String nodeDetailInfo = zkClient.getNodeData(servicePath + "/" + providerAddress);
                    nodeDetailInfoMap.put(providerAddress, nodeDetailInfo);
                }
                urlChangeWrapper.setNodeDataUrl(nodeDetailInfoMap);
                urlChangeWrapper.setProviderUrl(childrenDataList);
                urlChangeWrapper.setProviderUrl(childrenDataList);
                urlChangeWrapper.setServiceName(servicePath.split("/")[2]);
                IRpcEvent iRpcEvent = new IRpcUpdateEvent(urlChangeWrapper);
                IRpcListenerLoader.sendEvent(iRpcEvent);
                watchChildNodeData(servicePath);
                for (String providerAddress : childrenDataList) {
                    watchNodeDataChange(servicePath + "/" + providerAddress);
                }
            }
        });
    }


    @Override
    public void doBeforeSubscribe(URL url) {

    }

    @Override
    public List<String> getProviderIps(String serviceName) {
        List<String> nodeDataList = this.zkClient.getChildrenData(ROOT + "/" + serviceName + "/provider");
        return nodeDataList;
    }

    @Override
    public Map<String, String> getServiceWeightMap(String serviceName) {
        List<String> nodeDataList = this.zkClient.getChildrenData(ROOT + "/" + serviceName + "/provider");
        Map<String, String> result = new HashMap<>();
        for (String ipAndHost : nodeDataList) {
            String childData = this.zkClient.getNodeData(ROOT + "/" + serviceName + "/provider/" + ipAndHost);
            result.put(ipAndHost, childData);
        }
        return result;
    }

    //public static void main(String[] args) throws InterruptedException {
    //    ZookeeperRegister zookeeperRegister = new ZookeeperRegister("localhost:2181");
    //    List<String> urls = zookeeperRegister.getProviderIps(DataService.class.getName());
    //    System.out.println(urls);
    //    Thread.sleep(2000000);
    //}
}
