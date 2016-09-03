package com.dubboclub.dk.storage.model;

/**
 * Created by bieber on 2015/11/4.
 */
public class BaseItem implements java.io.Serializable{

    private String method;

    private String service;

    private long timestamp;

    private String remoteType;

    public String getRemoteType() {
        return remoteType;
    }

    public void setRemoteType(String remoteType) {
        this.remoteType = remoteType;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
