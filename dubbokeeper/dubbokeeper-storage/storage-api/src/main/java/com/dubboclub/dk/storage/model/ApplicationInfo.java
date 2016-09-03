package com.dubboclub.dk.storage.model;

/**
 * Created by bieber on 2015/11/16.
 */
public class ApplicationInfo extends  BaseInfo{

    private String applicationName;

    //0 消费者，1 提供者 2 既是消费者也是提供者
    private int applicationType;


    public int getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(int applicationType) {
        this.applicationType = applicationType;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

}
