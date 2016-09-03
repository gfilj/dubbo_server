package com.netease.dubbo_client.service;

public interface AssignmentTask {

	String newTask(String taskName);

	String cancelTask(String taskName);
}
