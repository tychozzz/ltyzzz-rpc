package com.ltyzzz.core.common;

import io.netty.channel.ChannelFuture;

public class ChannelFutureWrapper {

    private ChannelFuture channelFuture;

    private String host;

    private Integer port;

    private Integer weight;

    private String group;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public ChannelFutureWrapper(String host, Integer port, Integer weight) {
        this.host = host;
        this.port = port;
        this.weight = weight;
    }


    public ChannelFutureWrapper() {
    }

    public ChannelFuture getChannelFuture() {
        return channelFuture;
    }

    public void setChannelFuture(ChannelFuture channelFuture) {
        this.channelFuture = channelFuture;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }
}
