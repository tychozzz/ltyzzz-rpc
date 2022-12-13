package com.ltyzzz.core.server;

public class ServiceWrapper {

    private Object serviceObj;

    private String group = "default";

    private String serviceToken = "";

    private Integer limit = -1;

    public ServiceWrapper(Object serviceObj) {
        this.serviceObj = serviceObj;
    }

    public ServiceWrapper(Object serviceObj, String group) {
        this.serviceObj = serviceObj;
        this.group = group;
    }

    public String getServiceToken() {
        return serviceToken;
    }

    public void setServiceToken(String serviceToken) {
        this.serviceToken = serviceToken;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Object getServiceObj() {
        return serviceObj;
    }

    public void setServiceObj(Object serviceObj) {
        this.serviceObj = serviceObj;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}
