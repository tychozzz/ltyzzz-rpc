package com.ltyzzz.core.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RpcReferenceWrapper<T> {

    private Class<T> aimClass;

    private Map<String, Object> attachments = new ConcurrentHashMap<>();

    public Class<T> getAimClass() {
        return aimClass;
    }

    public void setAimClass(Class<T> aimClass) {
        this.aimClass = aimClass;
    }

    public boolean isAsync() {
        Object r = attachments.get("async");
        if (r == null || r.equals(false)) {
            return false;
        }
        return Boolean.valueOf(true);
    }

    public void setTimeOut(int timeOut) {
        attachments.put("timeOut", timeOut);
    }

    public String getTimeOUt() {
        return String.valueOf(attachments.get("timeOut"));
    }

    public int getRetry(){
        if(attachments.get("retry")==null){
            return 0;
        }else {
            return (int) attachments.get("retry");
        }
    }

    public void setRetry(int retry){
        this.attachments.put("retry",retry);
    }

    public void setAsync(boolean async) {
        this.attachments.put("async", async);
    }

    public String getUrl() {
        return String.valueOf(attachments.get("url"));
    }

    public void setUrl(String url) {
        this.attachments.put("url", url);
    }

    public String getServiceToken() {
        return String.valueOf(attachments.get("serviceToken"));
    }

    public void setServiceToken(String serviceToken) {
        this.attachments.put("serviceToken", serviceToken);
    }

    public String getGroup() {
        return String.valueOf(attachments.get("group"));
    }

    public void setGroup(String group) {
        this.attachments.put("group", group);
    }

    public Map<String, Object> getAttachments() {
        return attachments;
    }

    public void setAttachments(Map<String, Object> attachments) {
        this.attachments = attachments;
    }
}
